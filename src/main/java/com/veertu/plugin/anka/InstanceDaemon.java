package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.veertu.plugin.anka.AnkaMgmtCloud.Log;

public class InstanceDaemon implements EventHandler, Runnable {

    private Object mutex = new Object();
    private Map<String, VMNodeWrapper> instanceMap = new HashMap<>();
    private Map<String, VMNodeWrapper> nodeMap = new HashMap<>();

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
            VMNodeWrapper instance = new VMNodeWrapper();
            instance.vm = vm;
            instance.state = State.started;
            instance.noNodeCounter = 0;
            instanceMap.put(vm.getId(), instance);
            saveChanges();
        }
    }

    public void vmAttachedToNode(AbstractAnkaSlave node) {
        synchronized (mutex) {
            VMNodeWrapper instance = instanceMap.get(node.getVM().getId());
            instance.node = node;
            instance.noNodeCounter = -1;
            nodeMap.put(node.getNodeName(), instance);
            saveChanges();
        }
    }

    public void nodeTerminated(AbstractAnkaSlave node) {
        synchronized (mutex) {
            VMNodeWrapper instance = nodeMap.get(node.getNodeName());
            if (instance.state != State.pushing) {
                instance.state = State.shouldTerminate;
                nodeMap.remove(node.getNodeName());
                saveChanges();
            }
        }
    }

    public void saveImageSent(AbstractAnkaSlave node) {
        synchronized (mutex) {
            VMNodeWrapper instance = nodeMap.get(node.getNodeName());
            instance.state = State.pushing;
        }
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(5000);
                try {
                    Iterator<VMNodeWrapper> it = instanceMap.values().iterator();
                    while (it.hasNext()) {
                        VMNodeWrapper instance = it.next();
                        if (shouldRemoveInstance(instance)) {
                            it.remove();
                            saveChanges();
                        }
                        Thread.sleep(100); // 100 ms between each request
                    }
                } catch (Exception e) {
                    Log("Got exception running anka daemon instance loop");
                    e.printStackTrace();
                }
            } catch (Exception e) {
                Log("Got exception while waiting between anka daemon instance loop executions");
                e.printStackTrace();
            }
        }
    }

    private boolean shouldRemoveInstance(VMNodeWrapper instance) throws AnkaMgmtException {
        int NO_NODE_COUNT_LIMIT = 120;  // 10 mins approx
        try {
            String status = instance.vm.getStatus();
            if (status != null) {
                switch (instance.state) {
                case started:
                    switch (status) {
                    case "Started":
                        if (instance.node == null) {
                            Log("VM " + instance.vm.getId() + " is started and waiting for a node");
                            instance.noNodeCounter++;
                            if (instance.noNodeCounter > NO_NODE_COUNT_LIMIT) {
                                Log("Terminating VM " + instance.vm.getId() + " , gave up on getting a node");
                                instance.vm.terminate();
                                return true;
                            }
                        }
                    case "Scheduling":
                    case "Pulling":
                        return false;
                    case "Terminated":
                    case "Terminating":
                        Log("VM %s is in unexpected state %s (jenkins instance state: started)",
                                instance.vm.getId(), status);
                        return true;
                    }
                    Log("VM %s is in unexpected state %s (jenkins instance state: started)",
                            instance.vm.getId(), status);
                    break;
                case pushing:
                    if (status.equals("Pushing"))
                        return false;
                    Log("VM %s is in unexpected state: %s (jenkins instance state: pushing)",
                            instance.vm.getId(), status);
                    break;
                case shouldTerminate:
                    switch (status) {
                    case "Terminating":
                    case "Terminated":
                        return false;
                    default:
                        String vmID = instance.vm.getId();
                        Log("VM %s is in state %s (jenkins instance state: shouldTerminate)",
                                vmID, status);
                        Log("Terminating VM %s in 10 seconds", vmID);
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            Log("Sleep interrupted for vm %s, terminating now", vmID);
                        }
                        instance.vm.terminate();
                        Log("VM %s terminated", vmID);
                        return true;
                    }
                }
            }
        } catch (NullPointerException e) {
            Log("VM %s does not exist in controller", instance.vm.getId());
            if (instance.node != null) {
                nodeMap.remove(instance.node.getNodeName());
                saveChanges();
            }
            return true;
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
