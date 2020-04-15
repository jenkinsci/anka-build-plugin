package com.veertu.ankaMgmtSdk;

import org.apache.http.conn.ssl.TrustStrategy;

public class utils {

    public static TrustStrategy strategyLambda() {
        return (certificate, authType) -> true;
    }

}
