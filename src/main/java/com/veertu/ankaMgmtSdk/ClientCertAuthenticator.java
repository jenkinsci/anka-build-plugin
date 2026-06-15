package com.veertu.ankaMgmtSdk;

import hudson.util.Secret;
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

    private static final char[] KEYSTORE_ENTRY_PASSWORD = "somePassword".toCharArray();
    private static final String CERTIFICATE_ENTRY_ALIAS = "cert-alias";
    private static final String PRIVATE_ENTRY_ALIAS = "key-alias";

    private final String clientCert;
    private final Secret clientCertKey;
    private transient KeyStore keyStore;

    public ClientCertAuthenticator(String clientCert, String clientCertKey) {
        this.clientCert = clientCert;
        this.clientCertKey = Secret.fromString(clientCertKey);
    }

    public KeyStore makeTrustStore() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        Certificate certificate = parseClientCertificate(clientCert);
        PrivateKey privateKey = parseClientPrivateKey(clientCertKey.getPlainText());

        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(null);
        keystore.setCertificateEntry(CERTIFICATE_ENTRY_ALIAS, certificate);
        keystore.setKeyEntry(PRIVATE_ENTRY_ALIAS, privateKey, KEYSTORE_ENTRY_PASSWORD, new Certificate[] {certificate});
        return keystore;
    }

    static Certificate parseClientCertificate(String clientCertificatePem) throws CertificateException, IOException {
        if (clientCertificatePem == null) {
            throw new CertificateException("Client certificate PEM is null");
        }
        String trimmed = clientCertificatePem.trim();
        if (trimmed.isEmpty()) {
            throw new CertificateException("Client certificate PEM is empty");
        }

        try (PEMParser reader = new PEMParser(new StringReader(trimmed))) {
            final Object parsed;
            try {
                parsed = reader.readObject();
            } catch (Exception e) {
                throw new CertificateException(
                        "Client certificate is not valid PEM (could not decode). "
                                + "Expected a block starting with -----BEGIN CERTIFICATE-----",
                        e);
            }
            if (parsed == null) {
                throw new CertificateException(
                        "Client certificate is not valid PEM: no certificate was parsed. "
                                + "Expected a block starting with -----BEGIN CERTIFICATE-----");
            }
            if (!(parsed instanceof X509CertificateHolder)) {
                throw new CertificateException(
                        "Client certificate PEM must contain an X.509 certificate; found "
                                + parsed.getClass().getSimpleName()
                                + " instead");
            }
            try {
                return new JcaX509CertificateConverter()
                        .setProvider(new BouncyCastleProvider())
                        .getCertificate((X509CertificateHolder) parsed);
            } catch (Exception e) {
                throw new CertificateException("Client certificate PEM is not a valid X.509 certificate", e);
            }
        }
    }

    static PrivateKey parseClientPrivateKey(String clientKeyPem) throws CertificateException, IOException {
        if (clientKeyPem == null) {
            throw new CertificateException("Client key PEM is null");
        }
        String trimmed = clientKeyPem.trim();
        if (trimmed.isEmpty()) {
            throw new CertificateException("Client key PEM is empty");
        }

        try (PEMParser reader = new PEMParser(new StringReader(trimmed))) {
            final Object parsed;
            try {
                parsed = reader.readObject();
            } catch (Exception e) {
                throw new CertificateException(
                        "Client key is not valid PEM (could not decode). "
                                + "Expected a block starting with -----BEGIN ... PRIVATE KEY-----",
                        e);
            }
            if (parsed == null) {
                throw new CertificateException(
                        "Client key is not valid PEM: no private key was parsed. "
                                + "Expected a block starting with -----BEGIN ... PRIVATE KEY-----");
            }

            PrivateKeyInfo privateKeyInfo;
            if (parsed instanceof PrivateKeyInfo) {
                privateKeyInfo = (PrivateKeyInfo) parsed;
            } else if (parsed instanceof PEMKeyPair) {
                privateKeyInfo = ((PEMKeyPair) parsed).getPrivateKeyInfo();
            } else {
                throw new CertificateException(
                        "Client key PEM must contain a private key; found "
                                + parsed.getClass().getSimpleName()
                                + " instead");
            }

            try {
                return new JcaPEMKeyConverter()
                        .setProvider(new BouncyCastleProvider())
                        .getPrivateKey(privateKeyInfo);
            } catch (Exception e) {
                throw new CertificateException("Client key PEM is not a valid private key", e);
            }
        }
    }

    public KeyStore getKeyStore() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        if (this.keyStore == null) {
            this.keyStore = makeTrustStore();
        }
        return this.keyStore;
    }

    public char[] getKeyStoreEntryPassword() {
        return KEYSTORE_ENTRY_PASSWORD;
    }
}
