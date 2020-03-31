package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.ankaMgmtSdk.exceptions.VMDoesNotExistException;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.veertu.plugin.anka.AnkaMgmtCloud.Log;

public class InstanceDaemon implements EventHandler, Runnable {

    private transient Object mutex;
    private final transient Object runningLock = new Object();
    private transient Boolean isRunning;
    private Map<String, VMNodeWrapper> instanceMap = new HashMap<>();
    private Map<String, VMNodeWrapper> nodeMap = new HashMap<>();

    public InstanceDaemon() {
        mutex = new Object();
        isRunning = false;
    }

    public Object readResolve() {
        // v1.24.0 - changed mutex to transient and instantiation on constructor/load (niv)

        if (mutex != null)
            mutex = new Object();
        isRunning = false;
        removeInactiveNodes();  // pre v1.24.0 bug handling where nodeMap would grow in size
        return this;
    }

    private void removeInactiveNodes() {
        synchronized (mutex) {
            Iterator<String> it = nodeMap.keySet().iterator();

            while (it.hasNext()) {
                if (Jenkins.getInstance().getNode(it.next()) == null) {
                    it.remove();
                }
            }
        }
    }

    private void saveChanges() {
        try {
            Jenkins.getInstance().save();
        }
        catch (IOException e) {
            Log("Anka build plugin is not able to save cloud object to file. This could raise issues with ability to withstand Jenkins restarts");
            e.printStackTrace();
        }
    }

    public void vmStarted(AnkaMgmtVm vm) {
        synchronized (mutex) {
            VMNodeWrapper wrapper = new VMNodeWrapper();
            wrapper.vm = vm;
            wrapper.state = State.started;
            wrapper.noNodeCounter = 0;
            instanceMap.put(vm.getId(), wrapper);
            saveChanges();
        }
    }

    public void vmAttachedToNode(AbstractAnkaSlave node) {
        synchronized (mutex) {
            VMNodeWrapper wrapper = instanceMap.get(node.getVM().getId());
            if (wrapper != null ){
                wrapper.node = node;
                wrapper.noNodeCounter = -1;
                nodeMap.put(node.getNodeName(), wrapper);
                saveChanges();
            }
        }
    }

    public void nodeTerminated(AbstractAnkaSlave node) {
        // This should be kept idempotent
        synchronized (mutex) {
            VMNodeWrapper wrapper = nodeMap.get(node.getNodeName());
            if (wrapper != null && wrapper.state != State.pushing) {
                wrapper.state = State.shouldTerminate;
                nodeMap.remove(node.getNodeName());
                saveChanges();
            }
        }
    }

    public void saveImageSent(AbstractAnkaSlave node) {
        synchronized (mutex) {
            VMNodeWrapper wrapper = nodeMap.get(node.getNodeName());
            if (wrapper != null ) {
                wrapper.state = State.pushing;
                saveChanges();
            }
            else
                Log("Instance Daemon (save image event): Could not find node %s. Ignoring", node.getNodeName());
        }
    }

    public void run() {
        // Prevent from running more than once
        synchronized (runningLock) {
            if (isRunning) {
                Log("Instance daemon seems to be running already");
                return;
            }
            isRunning = true;
        }

        while (true) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Log("Got exception while waiting between anka daemon instance loop executions");
                e.printStackTrace();
            }
            synchronized (mutex) {
                boolean saveRequired = false;
                Iterator<VMNodeWrapper> it = instanceMap.values().iterator();

                while (it.hasNext()) {
                    VMNodeWrapper wrapper = it.next();
                    try {
                        if (shouldRemove(wrapper)) {
                            AbstractAnkaSlave node = wrapper.node;
                            nodeMap.remove(node.getNodeName());
                            it.remove();
                            saveRequired = true;
                        }
                    } catch (Exception e) {
                        Log("Got exception running anka daemon instance loop. Error: %s", e.getMessage());
                        e.printStackTrace();
                    }
                }
                if (saveRequired)
                    saveChanges();
            }
        }
    }

    private boolean shouldRemove(VMNodeWrapper wrapper) throws AnkaMgmtException {
        // This method decides if to remove the VM and Node from being monitored by the daemon
        // Termination of VMs is done in the slave/computer objects, here is only as fail safe or timeout

        final int NO_NODE_COUNT_LIMIT = 120;
        String vmStatus = "";
        String vmId = wrapper.vm.getId();

        // handle timeout for VM running but no Jenkins node
        if (wrapper.noNodeCounter > NO_NODE_COUNT_LIMIT) {
            Log("Terminating VM " + vmId + " , gave up on getting a node");
            wrapper.vm.terminate();
            return true;
        }

        try {
            vmStatus = wrapper.vm.getStatus();
        } catch (VMDoesNotExistException e) {
            return true;
        }

        switch (wrapper.state) {
        case started:
            if (vmStatus.equals("Scheduling") || vmStatus.equals("Pulling"))
                break;
            if (vmStatus.equals("Terminated") || vmStatus.equals("Terminating")) {
                Log("VM %s is in unexpected state %s (jenkins wrapper state: started)",
                        vmId, vmStatus);
                return true;
            }
            if (vmStatus.equals("Started")) {
                if (wrapper.node == null) {
                    Log("VM " + vmId + " is started and waiting for a node");
                    wrapper.noNodeCounter++;
                }
                break;
            }
            Log("VM %s is in unexpected state %s (jenkins wrapper state: started)",
                    vmId, vmStatus);
            break;
        case pushing:
            if (!vmStatus.equals("Pushing"))
                Log("VM %s is in unexpected state: %s (jenkins wrapper state: pushing)",
                        vmId, vmStatus);
            break;
        case shouldTerminate:
            if (vmStatus.equals("Terminating") || vmStatus.equals("Terminated"))
                break;

            Log("VM %s is in state %s (jenkins wrapper state: shouldTerminate)", vmId, vmStatus);
            try {
                Log("Terminating VM %s in 10 seconds", vmId);
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Log("Sleep interrupted for vm %s, terminating now", vmId);
            }
            wrapper.vm.terminate();
            return true;
        default:
            Log("Unknown state for vm %s wrapper: %s", vmId, wrapper.state);
        }
        return false;
    }

    @Override
    public void handle(AnkaEvent e) {
        String eventName = e.getClass().getCanonicalName();
        switch (eventName) {
        case "com.veertu.plugin.anka.NodeStarted":
            NodeStarted event = (NodeStarted) e;
            this.vmAttachedToNode(event.getNode());
            break;
        case "com.veertu.plugin.anka.VMStarted":
            VMStarted startEvent = (VMStarted) e;
            this.vmStarted(startEvent.getVm());
            break;
        case "com.veertu.plugin.anka.SaveImageEvent":
            SaveImageEvent saveEvent = (SaveImageEvent) e;
            this.saveImageSent(saveEvent.getNode());
            break;
        case "com.veertu.plugin.anka.NodeTerminated":
            NodeTerminated nodeTerminatedEvent = (NodeTerminated) e;
            this.nodeTerminated(nodeTerminatedEvent.getNode());
            break;
        default:
            Log("Could not identify event name. Got: %s ", eventName);
            break;
        }

    }

    private enum State {
        started, shouldTerminate, pushing
    }

    private class VMNodeWrapper {
        public State state;
        public AnkaMgmtVm vm;
        public AbstractAnkaSlave node; // i ♥ java
        public int noNodeCounter;
    }

}
