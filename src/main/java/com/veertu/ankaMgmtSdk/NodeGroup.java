package com.veertu.ankaMgmtSdk;


import org.json.JSONObject;

public class NodeGroup {

    private final String id;
    private final String name;
    private final String description;


    public NodeGroup(JSONObject jsonObject) {
        this.id = jsonObject.getString("id");
        this.name = jsonObject.getString("name");
        this.description = jsonObject.getString("description");
    }


    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}