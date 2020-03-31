package com.veertu.ankaMgmtSdk.exceptions;

public class VmAlreadyTerminatedException extends AnkaMgmtException {

    public VmAlreadyTerminatedException(String vmId) {
        super(String.format("VM %s already terminated", vmId));
    }
}
