package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaMgmtVm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class InstanceDaemon {

    private Object mutex = new Object();
    private Map<String, VMNodeWrapper> instanceMap = new HashMap<>();
    private Map<String, VMNodeWrapper> nodeMap = new HashMap<>();

    private enum State {
        started, shouldTerminate, pushing
    }

    private class VMNodeWrapper {
        public State state;
        public AnkaMgmtVm vm;
        public AbstractAnkaSlave node; // i ♥ java
        public int noNodeCounter;
    }

    public Map<String, String> getNodeNameToVmId() {
        HashMap<String, String> nodeToVm = new HashMap<>();

        Iterator<String> it = nodeMap.keySet().iterator();
        while (it.hasNext()) {
            String nodeName = it.next();
            VMNodeWrapper wrapper = nodeMap.get(nodeName);
            String vmId = wrapper.vm.getId();
            nodeToVm.put(nodeName, vmId);
        }

        return nodeToVm;
    }

    public Map<String, String> getVmIdToNodeName() {
        HashMap<String, String> vmToNode = new HashMap<>();

        Iterator<String> it = instanceMap.keySet().iterator();
        while (it.hasNext()) {
            String vmId = it.next();
            VMNodeWrapper wrapper = instanceMap.get(vmId);
            try {
                String nodeName = wrapper.node.getNodeName();
                vmToNode.put(vmId, nodeName);
            } catch (NullPointerException e) {
                vmToNode.put(vmId, null);
            }
        }

        return vmToNode;
    }

    public boolean isPushing(String vmId) {
        VMNodeWrapper wrapper = instanceMap.get(vmId);
        if (wrapper != null)
            return wrapper.state == State.pushing;

        Iterator<String> it = nodeMap.keySet().iterator();
        while (it.hasNext()) {
            String nodeName = it.next();
            wrapper = nodeMap.get(nodeName);
            if (wrapper.vm.getId().equals(vmId))
                return wrapper.state == State.pushing;
        }

        return false;

    }

}