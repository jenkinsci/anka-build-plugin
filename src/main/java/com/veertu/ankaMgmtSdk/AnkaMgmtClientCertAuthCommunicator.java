package com.veertu.ankaMgmtSdk;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

public class AnkaMgmtClientCertAuthCommunicator extends AnkaMgmtCommunicator {

    protected ClientCertAuthenticator authenticator;

    public AnkaMgmtClientCertAuthCommunicator(String mgmtUrl, String clientCert, String clientCertKey) {
        super(mgmtUrl);
        this.authenticator = new ClientCertAuthenticator(clientCert, clientCertKey);
    }

    public AnkaMgmtClientCertAuthCommunicator(String mgmtUrl, boolean skipTLSVerification, String client, String key) {
        super(mgmtUrl, skipTLSVerification);
        this.authenticator = new ClientCertAuthenticator(client, key);
    }

    protected CloseableHttpClient makeHttpClient() throws CertificateException, NoSuchAlgorithmException,
            KeyStoreException, IOException, UnrecoverableKeyException, KeyManagementException {
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder = requestBuilder.setConnectTimeout(timeout);
        requestBuilder = requestBuilder.setConnectionRequestTimeout(timeout);
        requestBuilder.setSocketTimeout(timeout);

        HttpClientBuilder builder = HttpClientBuilder.create();

        KeyStore keyStore = this.authenticator.getKeyStore();

        SSLContext sslContext = new SSLContextBuilder()
                .loadKeyMaterial(keyStore, authenticator.getPemPassword().toCharArray())
                .loadTrustMaterial(keyStore, utils.strategyLambda()).build();
        builder.setSSLContext(sslContext);

        setTLSVerificationIfDefined(sslContext, builder);
        CloseableHttpClient httpClient = builder.setDefaultRequestConfig(requestBuilder.build()).build();
        return httpClient;
    }
}
