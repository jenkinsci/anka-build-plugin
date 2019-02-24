package com.veertu.ankaMgmtSdk.exceptions;

public class AnkaUnAuthenticatedRequestException extends AnkaMgmtException {
    public AnkaUnAuthenticatedRequestException(Throwable e) {
        super(e);
    }

    public AnkaUnAuthenticatedRequestException(String s) {
        super(s);
    }
}
