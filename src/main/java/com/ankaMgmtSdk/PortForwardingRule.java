package com.veertu.ankaMgmtSdk;

import org.json.JSONObject;

/**
 * Created by asafgur on 17/05/2017.
 */
public class PortForwardingRule {

    private final int hostPort;
    private final int guestPort;

    public PortForwardingRule(JSONObject portForwardingJsonObj) {
        this.hostPort = portForwardingJsonObj.getInt("host_port");
        this.guestPort = portForwardingJsonObj.getInt("guest_port");

    }

    public int getHostPort() {
        return hostPort;
    }

    public int getGuestPort() {
        return guestPort;
    }
}
