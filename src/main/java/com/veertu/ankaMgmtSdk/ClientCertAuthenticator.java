package com.veertu.ankaMgmtSdk;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

public class ClientCertAuthenticator {

    private final String clientCert;
    private final String clientCertKey;
    private final String pemPassword = "somePassword";
    private final String keyAlias = "key-alias";
    private final String certAlias = "cert-alias";
    private transient KeyStore keyStore;

    public ClientCertAuthenticator(String clientCert, String clientCertKey) {
        this.clientCert = clientCert;
        this.clientCertKey = clientCertKey;
    }

    public KeyStore makeTrustStore() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        try {
            PEMParser reader;
            BouncyCastleProvider bouncyCastleProvider = new BouncyCastleProvider();

            reader = new PEMParser(new StringReader(clientCert));
            X509CertificateHolder holder = (X509CertificateHolder)reader.readObject();
            Certificate certificate = new JcaX509CertificateConverter().setProvider(bouncyCastleProvider).getCertificate(holder);

            reader = new PEMParser(new StringReader(clientCertKey));
            Object obj = reader.readObject();
            PrivateKeyInfo privateKeyInfo;
            if (obj instanceof PrivateKeyInfo) {
                privateKeyInfo = (PrivateKeyInfo) obj;
            } else if (obj instanceof PEMKeyPair) {
                privateKeyInfo = ((PEMKeyPair)obj).getPrivateKeyInfo();
            } else {
                throw new IllegalArgumentException("Invalid private key format");
            }

            PrivateKey privateKey = new JcaPEMKeyConverter().setProvider(bouncyCastleProvider).getPrivateKey(privateKeyInfo);

            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(null);
            keystore.setCertificateEntry(certAlias, certificate);
            keystore.setKeyEntry(keyAlias, privateKey, pemPassword.toCharArray(), new Certificate[] {certificate});
            return keystore;

        } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public KeyStore getKeyStore() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        if (this.keyStore == null) {
            this.keyStore = makeTrustStore();
        }
        return this.keyStore;
    }


    public String getPemPassword() {
        return pemPassword;
    }
}
