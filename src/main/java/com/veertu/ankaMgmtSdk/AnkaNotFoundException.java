package com.veertu.ankaMgmtSdk;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;

public class AnkaNotFoundException extends AnkaMgmtException {
    public AnkaNotFoundException(Throwable e) {
        super(e);
    }

    public AnkaNotFoundException(String e) {
        super(e);
    }
}
