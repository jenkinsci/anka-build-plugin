package com.veertu.ankaMgmtSdk.exceptions;


public class ClientException extends Throwable {

    public ClientException(Throwable reason) {
        super(reason);
    }

    public ClientException(String message) {
        super(message);
    }
}
