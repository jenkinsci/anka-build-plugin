package com.veertu.ankaMgmtSdk;

import org.json.JSONObject;

/**
 * Created by asafgur on 17/05/2017.
 */
public class AnkaVmSession extends AnkaVMRepresentation {

    private final String sessionState;
    private final String vmId;
    private AnkaVmInfo vmInfo;

    public AnkaVmSession(String id, JSONObject jsonObject) {
        this.id = id;
        this.sessionState = jsonObject.getString("instance_state");
        this.vmId = jsonObject.getString("vmid");
        if (jsonObject.has("vminfo")) {
            JSONObject ankaVmInfo = jsonObject.getJSONObject("vminfo");
            this.vmInfo = new AnkaVmInfo(ankaVmInfo);
        }
    }


    public String getSessionState() {
        return sessionState;
    }

    public String getVmId() {
        return vmId;
    }

    public AnkaVmInfo getVmInfo() {
        return vmInfo;
    }
}


