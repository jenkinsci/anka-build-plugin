package com.veertu.ankaMgmtSdk;

import org.apache.http.conn.ssl.TrustStrategy;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class utils {

    public static TrustStrategy strategyLambda() {
        return new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] certificate, String authType) throws CertificateException {
                return true;
            }
        };
    }

}
