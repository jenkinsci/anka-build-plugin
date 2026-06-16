package com.veertu.ankaMgmtSdk.exceptions;


public class ClientException extends Exception {

    public ClientException(Throwable reason) {
        super(reason);
    }

    public ClientException(String message) {
        super(message);
    }
}
