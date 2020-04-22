package com.veertu.ankaMgmtSdk;

import org.json.JSONObject;

/**
 * Created by asafgur on 17/05/2017.
 */
public class AnkaVmInstance extends AnkaVMRepresentation {

    private final String sessionState;
    private final String vmId;
    private AnkaVmInfo vmInfo;
    private String message;

    public AnkaVmInstance(String id, JSONObject jsonObject) {
        this.id = id;
        this.sessionState = jsonObject.getString("instance_state");
        this.vmId = jsonObject.getString("vmid");
        if (jsonObject.has("vminfo")) {
            JSONObject ankaVmInfo = jsonObject.getJSONObject("vminfo");
            this.vmInfo = new AnkaVmInfo(ankaVmInfo);
        }
        if (jsonObject.has("message")) {
            this.message = jsonObject.getString("message");
        }
    }

    public static AnkaVmInstance makeAnkaVmSessionFromJson(JSONObject jsonObject) {
        String instance_id = jsonObject.getString("instance_id");
        JSONObject vm = jsonObject.getJSONObject("vm");
        return new AnkaVmInstance(instance_id, vm);
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

    public String getMessage() {
        return this.message;
    }

    public boolean isTerminatingOrTerminated() {
        switch (sessionState) {
            case "Terminated":
            case "Terminating":
                return true;
        }
        return false;
    }

    public boolean isStarted() {
        return sessionState.equals("Started");
    }

    public boolean isSchedulingOrPulling() {
        switch (sessionState) {
            case "Pulling":
            case "Scheduling":
                return true;
        }
        return false;
    }

    public boolean isInError() {
        return sessionState.equalsIgnoreCase("error");
    }
}


