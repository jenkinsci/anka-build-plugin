package com.veertu.ankaMgmtSdk;

import org.apache.http.conn.ssl.TrustStrategy;

public final class Utils {

    private Utils() {
    }

    public static TrustStrategy strategyLambda() {
        return (certificate, authType) -> true;
    }

}
