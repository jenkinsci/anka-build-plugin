package com.veertu.ankaMgmtSdk;

import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.List;

public class AnkaMgmtClientCertAuthCommunicator extends AnkaMgmtCommunicator {

    protected ClientCertAuthenticator authenticator;

    public AnkaMgmtClientCertAuthCommunicator(String mgmtUrl, boolean skipTLSVerification, String client, String key, String rootCA) {
        super(mgmtUrl, skipTLSVerification, rootCA);
        this.authenticator = new ClientCertAuthenticator(client, key);
    }

    public AnkaMgmtClientCertAuthCommunicator(List<String> mgmtURLS, boolean skipTLSVerification, String client, String key, String rootCA) {
        super(mgmtURLS, skipTLSVerification, rootCA);
        this.authenticator = new ClientCertAuthenticator(client, key);
    }

    @Override
    protected KeyStore getKeyStore() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        return this.authenticator.getKeyStore();
    }

    @Override
    protected SSLContext getSSLContext(KeyStore keystore) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException {
        return new SSLContextBuilder()
                .loadKeyMaterial(keystore, authenticator.getPemPassword().toCharArray())
                .loadTrustMaterial(keystore, getTrustStrategy()).build();
    }
}
