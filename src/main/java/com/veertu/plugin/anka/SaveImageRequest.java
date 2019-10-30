package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;

public class SaveImageRequest {

    public String getBuildId() {
        return buildId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String reqId) {
        this.requestId = reqId;
        state = SaveImageState.Pending;
    }

    public AnkaMgmtCloud getCloud() {
        return cloud;
    }

    private final AnkaMgmtCloud cloud;
    private final String buildId;
    private String requestId;
    private SaveImageState state;

    private long timeout = 10 * 60 * 1000; // 10 minutes
    private long created;

    public SaveImageRequest(AnkaMgmtCloud cloud, String buildId) {
        this.created = System.currentTimeMillis();
        this.cloud = cloud;
        this.state = SaveImageState.Requesting;
        this.buildId = buildId;
    }

    public SaveImageState checkState() {
        switch (state) {
            case Requesting:
                if (System.currentTimeMillis() - created > timeout) {
                        state = SaveImageState.Timeout;
                }
                return state;
            case Pending:
                getRemoteState();
            case Done:
            case Error:
            case Timeout:
                return state;
        }
        return state;
    }

    private void getRemoteState() {
        try {

            String status = cloud.getAnkaApi().getSaveImageStatus(requestId);
            if (status.toLowerCase().equals("pending")) {
                state = SaveImageState.Pending;
            }
            if (status.toLowerCase().equals("done")) {
                state = SaveImageState.Done;
            }
            if (status.toLowerCase().equals("error")) {
                state = SaveImageState.Error;
            }
        } catch (AnkaMgmtException e) {
            state = SaveImageState.Pending;
        }
    }

}
