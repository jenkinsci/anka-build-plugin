package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;

import java.util.HashMap;
import java.util.Map;

public class InstanceDaemon implements EventHandler, Runnable {

//    private List<AnkaMgmtVm> instances = new LinkedList<>();
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
                for (VMNodeWrapper instance: instanceMap.values()) {
                    checkInstance(instance);
                    Thread.sleep(100); // 100 ms between each request
                }
                Thread.sleep(5000); // run every 5 seconds
            } catch (Exception e) {
                AnkaMgmtCloud.Log("Got exception running instance loop");
                e.printStackTrace();
            }
        }
    }

    private void checkInstance(VMNodeWrapper instance) throws AnkaMgmtException {
        String status = instance.vm.getStatus();
        if (status != null) {
            switch (instance.state) {
                case started:
                    switch (status) {
                        case "Scheduling":
                        case "Pulling":
                        case "Started":
                            return;
                    }
                    AnkaMgmtCloud.Log("VM %s is in unexpected state %s", instance.vm.getId(), status);

                    break;
                case pushing:
                    switch (status) {
                        case "Pushing":
                            return;
                    }
                    AnkaMgmtCloud.Log("VM %s is in unexpected state %s", instance.vm.getId(), status);
                    break;
                case shouldTerminate:
                    switch (status) {
                        case "Terminating":
                        case "Terminated":
                            return;
                        case "Started":
                        case "Scheduling":
                            instance.vm.terminate();
                            return;
                    }
                    AnkaMgmtCloud.Log("VM %s is in unexpected state %s", instance.vm.getId(), status);
                    break;
            }
        }

    }

    @Override
    public void handle(AnkaEvent e) {

        switch (e.getClass().getCanonicalName()) {
            case "com.veertu.plugin.anka.events.NodeStarted":
                NodeStarted event = (NodeStarted) e;
                this.vmAttachedToNode(event.getNode());
                break;
            case "com.veertu.plugin.anka.events.VMStarted":
                VMStarted startEvent = (VMStarted) e;
                this.vmStarted(startEvent.getVm());
                break;
            case "com.veertu.plugin.anka.events.SaveImageEvent":
                SaveImageEvent saveEvent = (SaveImageEvent) e;
                this.saveImageSent(saveEvent.getNode());
                break;
            case "com.veertu.plugin.anka.events.NodeTerminated":
                NodeTerminated nodeTerminatedEvent = (NodeTerminated) e;
                this.nodeTerminated(nodeTerminatedEvent.getNode());
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
