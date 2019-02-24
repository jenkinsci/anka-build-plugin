package com.veertu.ankaMgmtSdk;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Asaf Gur.
 */

public class AnkaCloudStatus {

    protected String status;
    protected String version;
    protected String registryAddress;
    protected String registryStatus;

    public AnkaCloudStatus(String status, String version, String registryAddress, String registryStatus) {
        this.status = status;
        this.version = version;
        this.registryAddress = registryAddress;
        this.registryStatus = registryStatus;
    }

    public String getStatus() {
        return status;
    }

    public String getVersion() {
        return version;
    }

    public String getRegistryAddress() {
        return registryAddress;
    }

    public String getRegistryStatus() {
        return registryStatus;
    }


    public static AnkaCloudStatus fromJson(JSONObject statusJson) {
       String status = getJsonStringOrNull(statusJson,"status");
       String version = getJsonStringOrNull(statusJson,"version");
       String registryAddress = getJsonStringOrNull(statusJson,"registry_address");
       String registryStatus = getJsonStringOrNull(statusJson,"registry_status");
       return new AnkaCloudStatus(status, version, registryAddress, registryStatus);
    }

    private static String getJsonStringOrNull(JSONObject jsonObject, String key) {
        try {
            return jsonObject.getString(key);
        } catch (JSONException e) {
            return null;
        }
    }
}
