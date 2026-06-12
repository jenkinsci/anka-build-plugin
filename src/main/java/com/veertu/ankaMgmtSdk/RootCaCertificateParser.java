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
    private static final String BEGIN_CERTIFICATE = "-----BEGIN CERTIFICATE-----";
    private static final String END_CERTIFICATE = "-----END CERTIFICATE-----";

    private RootCaCertificateParser() {}

    static Certificate parsePemToCertificate(String rootCaPem) throws CertificateException, IOException {
        if (rootCaPem == null) {
            throw new CertificateException("Root CA PEM is null");
        }
        String trimmed = rootCaPem.trim();
        if (trimmed.isEmpty()) {
            throw new CertificateException("Root CA PEM is empty");
        }

        try (PEMParser reader = new PEMParser(new StringReader(normalizeCertificatePem(trimmed)))) {
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

    private static String normalizeCertificatePem(String pem) throws CertificateException {
        int begin = pem.indexOf(BEGIN_CERTIFICATE);
        if (begin < 0) {
            return pem;
        }

        int payloadStart = begin + BEGIN_CERTIFICATE.length();
        int end = pem.indexOf(END_CERTIFICATE, payloadStart);
        if (end < 0) {
            throw new CertificateException("Root CA PEM is missing " + END_CERTIFICATE);
        }

        String prefix = pem.substring(0, begin);
        String suffix = pem.substring(end + END_CERTIFICATE.length());
        if (!prefix.trim().isEmpty() || !suffix.trim().isEmpty()) {
            throw new CertificateException("Root CA PEM must contain exactly one certificate block");
        }

        String encodedCertificate = pem.substring(payloadStart, end).replaceAll("\\s+", "");
        return BEGIN_CERTIFICATE + "\n" + encodedCertificate + "\n" + END_CERTIFICATE;
    }
}
