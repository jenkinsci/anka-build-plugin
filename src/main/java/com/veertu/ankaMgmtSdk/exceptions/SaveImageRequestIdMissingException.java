package com.veertu.ankaMgmtSdk.exceptions;

public class SaveImageRequestIdMissingException extends AnkaMgmtException {

    public SaveImageRequestIdMissingException(String reqId) {
        super("Information about save image req id %s is no longer available");
    }
}
