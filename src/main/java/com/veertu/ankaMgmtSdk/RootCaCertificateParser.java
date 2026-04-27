package com.veertu.ankaMgmtSdk;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;

import java.io.IOException;
import java.io.StringReader;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

/**
 * Parses a controller Root CA from PEM text. Rejects empty, non-PEM, or non-certificate content
 * with a clear {@link CertificateException} instead of failing later with a {@link NullPointerException}.
 */
final class RootCaCertificateParser {

    private RootCaCertificateParser() {}

    static Certificate parsePemToCertificate(String rootCaPem) throws CertificateException, IOException {
        if (rootCaPem == null) {
            throw new CertificateException("Root CA PEM is null");
        }
        String trimmed = rootCaPem.trim();
        if (trimmed.isEmpty()) {
            throw new CertificateException("Root CA PEM is empty");
        }

        try (PEMParser reader = new PEMParser(new StringReader(trimmed))) {
            final Object parsed;
            try {
                parsed = reader.readObject();
            } catch (Exception e) {
                throw new CertificateException(
                        "Root CA is not valid PEM (could not decode). "
                                + "Expected a block starting with -----BEGIN CERTIFICATE-----",
                        e);
            }
            if (parsed == null) {
                throw new CertificateException(
                        "Root CA is not valid PEM: no certificate was parsed. "
                                + "Expected a block starting with -----BEGIN CERTIFICATE-----");
            }
            if (!(parsed instanceof X509CertificateHolder)) {
                throw new CertificateException(
                        "Root CA PEM must contain an X.509 certificate; found "
                                + parsed.getClass().getSimpleName()
                                + " instead");
            }
            try {
                return new JcaX509CertificateConverter()
                        .setProvider(new BouncyCastleProvider())
                        .getCertificate((X509CertificateHolder) parsed);
            } catch (Exception e) {
                throw new CertificateException("Root CA PEM is not a valid X.509 certificate", e);
            }
        }
    }
}
