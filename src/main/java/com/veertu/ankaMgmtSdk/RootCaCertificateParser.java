package com.veertu.ankaMgmtSdk;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;

import java.io.IOException;
import java.io.StringReader;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a controller Root CA from PEM text. Rejects empty, non-PEM, or non-certificate content
 * with a clear {@link CertificateException} instead of failing later with a {@link NullPointerException}.
 *
 * <p>Some credential stores (notably the Jenkins "Secret text" single-line field) collapse the
 * newlines of a pasted PEM into spaces, which {@link PEMParser} cannot decode. To stay backward
 * compatible the PEM is first parsed exactly as provided; only when that fails do we attempt to
 * repair whitespace-flattened blocks and retry. Inputs that parsed before this repair existed
 * (certificate bundles, PEMs surrounded by OpenSSL metadata, etc.) are therefore never rejected.
 */
final class RootCaCertificateParser {

    /** Matches a single PEM block, capturing the label (group 1) and the base64 body (group 2). */
    private static final Pattern PEM_BLOCK = Pattern.compile(
            "-----BEGIN ([A-Z0-9 ]+?)-----(.*?)-----END \\1-----",
            Pattern.DOTALL);

    private static final int PEM_LINE_LENGTH = 64;

    private RootCaCertificateParser() {}

    static Certificate parsePemToCertificate(String rootCaPem) throws CertificateException, IOException {
        if (rootCaPem == null) {
            throw new CertificateException("Root CA PEM is null");
        }
        String trimmed = rootCaPem.trim();
        if (trimmed.isEmpty()) {
            throw new CertificateException("Root CA PEM is empty");
        }

        try {
            return readCertificate(trimmed);
        } catch (CertificateException originalFailure) {
            String repaired = repairFlattenedPem(trimmed);
            if (repaired == null) {
                throw originalFailure;
            }
            try {
                return readCertificate(repaired);
            } catch (CertificateException repairDidNotHelp) {
                // Surface the original failure so the message reflects exactly what the user provided.
                throw originalFailure;
            }
        }
    }

    private static Certificate readCertificate(String pem) throws CertificateException, IOException {
        try (PEMParser reader = new PEMParser(new StringReader(pem))) {
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

    /**
     * Rebuilds any PEM blocks whose base64 body had its line breaks collapsed (e.g. into spaces) so
     * {@link PEMParser} can decode them. Returns {@code null} when no PEM block is present, signalling
     * that no repair is possible and the original parse failure should stand.
     */
    private static String repairFlattenedPem(String pem) {
        Matcher matcher = PEM_BLOCK.matcher(pem);
        StringBuilder rebuilt = new StringBuilder();
        boolean foundBlock = false;
        while (matcher.find()) {
            String label = matcher.group(1);
            String body = matcher.group(2).replaceAll("\\s+", "");
            if (body.isEmpty()) {
                continue;
            }
            foundBlock = true;
            rebuilt.append("-----BEGIN ").append(label).append("-----\n");
            for (int i = 0; i < body.length(); i += PEM_LINE_LENGTH) {
                rebuilt.append(body, i, Math.min(i + PEM_LINE_LENGTH, body.length())).append('\n');
            }
            rebuilt.append("-----END ").append(label).append("-----\n");
        }
        return foundBlock ? rebuilt.toString() : null;
    }
}
