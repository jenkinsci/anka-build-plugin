package com.veertu.ankaMgmtSdk;

import org.json.JSONObject;

public class AnkaNode {


    private final String nodeId;
    private final String state;
    private final String capacityMode;
    private final int capacity;

    public String getNodeId() {
        return nodeId;
    }

    public String getState() {
        return state;
    }

    public String getCapacityMode() {
        return capacityMode;
    }

    public int getCapacity() {
        return capacity;
    }


    public AnkaNode(String nodeId, String state, String capacityMode, int capacity) {
        this.nodeId = nodeId;
        this.state = state;
        this.capacityMode = capacityMode;
        this.capacity = capacity;
    }

    public static AnkaNode fromJson(JSONObject nodeJson) {
        String nodeId = nodeJson.optString("node_id");
        String state = nodeJson.optString("state");
        String capacityMode = nodeJson.optString("capacity_mode");
        int capacity = nodeJson.optInt("capacity");
        return new AnkaNode(nodeId, state, capacityMode, capacity);
    }


    public boolean isActive() {
        return state.equalsIgnoreCase("active");
    }

    public boolean hasQuantityBasedCapacity() {
        return capacityMode.equalsIgnoreCase("number") ||
                !capacityMode.equalsIgnoreCase("resource");
    }
}
