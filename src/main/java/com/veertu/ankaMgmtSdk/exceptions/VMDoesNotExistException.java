package com.veertu.ankaMgmtSdk.exceptions;

public class VMDoesNotExistException extends AnkaMgmtException {

    public VMDoesNotExistException(String vmId) {
        super(String.format("VM %s does not exist", vmId));
    }
}
