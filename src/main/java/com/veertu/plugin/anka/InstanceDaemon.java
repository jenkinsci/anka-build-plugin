package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class InstanceDaemon implements EventHandler, Runnable {

    private final Object mutex = new Object();
    private Map<String, VMNodeWrapper> instanceMap = new HashMap<>();
    private Map<String, VMNodeWrapper> nodeMap = new HashMap<>();

    public void vmStarted(AnkaMgmtVm vm) {
        synchronized (mutex) {
            VMNodeWrapper instance = new VMNodeWrapper();
            instance.vm = vm;
            instance.state = State.started;
            instanceMap.put(vm.getId(), instance);
        }
    }

    public void vmAttachedToNode(AbstractAnkaSlave node) {
        synchronized (mutex) {
            VMNodeWrapper instance = instanceMap.get(node.getVM().getId());
            instance.node = node;
            nodeMap.put(node.getNodeName(), instance);
        }
    }

    public void nodeTerminated(AbstractAnkaSlave node) {
        synchronized (mutex) {
            VMNodeWrapper instance = nodeMap.get(node.getNodeName());
            if (instance.state != State.pushing) {
                instance.state = State.shouldTerminate;
                nodeMap.remove(node.getNodeName());
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
                        if (removeInstance(instance)) {
                            it.remove();
                        }
                        Thread.sleep(100); // 100 ms between each request
                    }
                } catch (Exception e) {
                    AnkaMgmtCloud.Log("Got exception running anka daemon instance loop");
                    e.printStackTrace();
                }
            } catch (Exception e) {
                AnkaMgmtCloud.Log("Got exception while waiting between anka daemon instance loop executions");
                e.printStackTrace();
            }
        }
    }

    private boolean removeInstance(VMNodeWrapper instance) throws AnkaMgmtException {
        try {
            String status = instance.vm.getStatus();
            if (status != null) {
                switch (instance.state) {
                    case started:
                        switch (status) {
                            case "Scheduling":
                            case "Pulling":
                            case "Started":
                                return false;
                            case "Terminated":
                            case "Terminating":
                                AnkaMgmtCloud.Log("VM %s is in unexpected state %s (jenkins instance state: started)", instance.vm.getId(), status);
                                return true;
                        }
                        AnkaMgmtCloud.Log("VM %s is in unexpected state %s (jenkins instance state: started)", instance.vm.getId(), status);
                        break;
                    case pushing:
                        switch (status) {
                            case "Pushing":
                                return false;
                        }
                        AnkaMgmtCloud.Log("VM %s is in unexpected state: %s (jenkins instance state: pushing)", instance.vm.getId(), status);
                        break;
                    case shouldTerminate:
                        switch (status) {
                            case "Terminating":
                            case "Terminated":
                                return false;
                            default:
                                AnkaMgmtCloud.Log("VM %s is in unexpected state %s (jenkins instance state: shouldTerminate)", instance.vm.getId(), status);
                                AnkaMgmtCloud.Log("Terminating VM %s", instance.vm.getId());
                                instance.vm.terminate();
                                return true;
                        }
                }
            }
        } catch (NullPointerException e) {
            AnkaMgmtCloud.Log("VM %s does not exist in controller", instance.vm.getId());
            if (instance.node != null)
                nodeMap.remove(instance.node.getNodeName());
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
                AnkaMgmtCloud.Log("Could not identify event name. Got: %s ", eventName);
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
    }

}
