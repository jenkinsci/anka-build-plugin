package com.veertu.ankaMgmtSdk;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AnkaVmInfo {

    private final String uuid;
    private final String name;
    private final String status;
    private final String vmIp;
    private final List<PortForwardingRule> portForwardingRules;

    private final String hostIp;

    public AnkaVmInfo(JSONObject jsonObject) {
        this.uuid = jsonObject.getString("uuid");
        this.name = jsonObject.getString("name");
        this.status = jsonObject.getString("status");
        this.vmIp = jsonObject.optString("ip");
        this.hostIp = jsonObject.optString("host_ip");
        this.portForwardingRules = new ArrayList<>();
        if (!jsonObject.isNull("port_forwarding")) {
            JSONArray portForwardRulesJson = jsonObject.getJSONArray("port_forwarding");
            for (int i=0; i < portForwardRulesJson.length(); i++) {
                this.portForwardingRules.add(new PortForwardingRule(portForwardRulesJson.getJSONObject(i)));
            }
        }
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public String getVmIp() {
        return vmIp;
    }

    public String getHostIp() {
        return hostIp;
    }

    public List<PortForwardingRule> getPortForwardingRules() {
        return portForwardingRules;
    }
}
