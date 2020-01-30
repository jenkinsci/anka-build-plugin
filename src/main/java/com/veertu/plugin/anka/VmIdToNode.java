package com.veertu.plugin.anka;

import java.util.HashMap;
import java.util.Map;

public class VmIdToNode {

    private Map<String, String> vmIdToNodeMap = new HashMap<>();

    public VmIdToNode() {
    }

    public void add(String vmId, String nodeName) {
        vmIdToNodeMap.put(nodeName, vmId);
    }

    public String remove(String nodeName) {
        return vmIdToNodeMap.remove(nodeName);
    }

    public String findVmId(String nodeName) {
        return vmIdToNodeMap.get(nodeName);
    }
}
