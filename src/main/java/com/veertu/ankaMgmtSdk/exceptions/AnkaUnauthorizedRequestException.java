package com.veertu.ankaMgmtSdk.exceptions;

public class AnkaUnauthorizedRequestException extends AnkaMgmtException {
    public AnkaUnauthorizedRequestException(Throwable e) {
        super(e);
    }

    public AnkaUnauthorizedRequestException(String message) {
        super(message);
    }
}
