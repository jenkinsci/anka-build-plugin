package com.veertu.ankaMgmtSdk;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.StringReader;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.List;

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

    public AnkaMgmtClientCertAuthCommunicator(String mgmtUrl, boolean skipTLSVerification, String client, String key, String rootCA) {
        super(mgmtUrl, skipTLSVerification, rootCA);
        this.authenticator = new ClientCertAuthenticator(client, key);
    }

    public AnkaMgmtClientCertAuthCommunicator(List<String> mgmtURLS, boolean skipTLSVerification, String client, String key, String rootCA) {
        super(mgmtURLS, skipTLSVerification, rootCA);
        this.authenticator = new ClientCertAuthenticator(client, key);
    }

    protected CloseableHttpClient makeHttpClient(int reqTimeout) throws CertificateException, NoSuchAlgorithmException,
            KeyStoreException, IOException, UnrecoverableKeyException, KeyManagementException {
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder = requestBuilder.setConnectTimeout(reqTimeout);
        requestBuilder = requestBuilder.setConnectionRequestTimeout(reqTimeout);
        requestBuilder.setSocketTimeout(reqTimeout);

        HttpClientBuilder builder = HttpClientBuilder.create();

        KeyStore keyStore = this.authenticator.getKeyStore();
        if (rootCA != null) {
            PEMParser reader;
            BouncyCastleProvider bouncyCastleProvider = new BouncyCastleProvider();
            reader = new PEMParser(new StringReader(rootCA));
            X509CertificateHolder holder = (X509CertificateHolder)reader.readObject();
            Certificate certificate = new JcaX509CertificateConverter().setProvider(bouncyCastleProvider).getCertificate(holder);
            keyStore.setCertificateEntry("rootCA", certificate);
        }

        SSLContext sslContext = new SSLContextBuilder()
                .loadKeyMaterial(keyStore, authenticator.getPemPassword().toCharArray())
                .loadTrustMaterial(keyStore, getTrustStartegy()).build();
        builder.setSSLContext(sslContext);

        setTLSVerificationIfDefined(sslContext, builder);
        builder.disableAutomaticRetries();
        CloseableHttpClient httpClient = builder.setDefaultRequestConfig(requestBuilder.build()).build();
        return httpClient;
    }
}
