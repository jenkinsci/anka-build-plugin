package com.veertu.ankaMgmtSdk;

import org.junit.Test;

import java.security.cert.CertificateException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class RootCaCertificateParserTest {
    private static final String VALID_CERTIFICATE = """
            -----BEGIN CERTIFICATE-----
            MIICIjCCAYugAwIBAgIUHLdi0rDaq6sxT0Ma31TzyyiSTUIwDQYJKoZIhvcNAQEL
            BQAwIzEhMB8GA1UEAwwYQW5rYSBSb290IENBIFBhcnNlciBUZXN0MB4XDTI2MDYx
            MjA5NDMwOVoXDTI2MDYxMzA5NDMwOVowIzEhMB8GA1UEAwwYQW5rYSBSb290IENB
            IFBhcnNlciBUZXN0MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDNIRg8PRth
            PzrEbfyZVZsmu0gR54ZnUuiEOpQgvTSzdp/cSwlDrKqUZgT028WfhHJEQ3LHPkOH
            9tA4tXrhTPndEW3dRei8uLXRyZ4l7kPfY2HHxpNzxP7Mz+KRUs+DcTpQ09vA5lRr
            h3V6cBRHOUXrfvOAKf46I+EsHAzZLptkNwIDAQABo1MwUTAdBgNVHQ4EFgQUc4cH
            mY3ynStmdp3N+hZmfNQIiMEwHwYDVR0jBBgwFoAUc4cHmY3ynStmdp3N+hZmfNQI
            iMEwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOBgQBxAyVjGu3RXpsC
            mt85wd94TeeiCtcIyv0P1Pv7vYdwJ3KQyWolowJuhjUMmzOyH/6KxL58GqHIxCbx
            1SaM/4eOC2vy+95F7fc8lYW6d85D97KLK96PILYAveo8UjWfjZSaP1eX1xnCsHyS
            eADezoB6Qtc4k2qKCXmv65EMFuYO9g==
            -----END CERTIFICATE-----
            """;

    @Test
    public void parsePemToCertificate_acceptsMultilinePem() throws Exception {
        assertNotNull(RootCaCertificateParser.parsePemToCertificate(VALID_CERTIFICATE));
    }

    @Test
    public void parsePemToCertificate_acceptsSpaceFlattenedPem() throws Exception {
        assertNotNull(RootCaCertificateParser.parsePemToCertificate(
                VALID_CERTIFICATE.replaceAll("\\R", " ")));
    }

    @Test
    public void parsePemToCertificate_rejectsTextOutsideCertificateBlock() {
        CertificateException ex = assertThrows(
                CertificateException.class,
                () -> RootCaCertificateParser.parsePemToCertificate("unexpected " + VALID_CERTIFICATE));
        assertThat(ex.getMessage(), containsString("exactly one certificate block"));
    }

    @Test
    public void parsePemToCertificate_rejectsNonPemText() {
        CertificateException ex = assertThrows(
                CertificateException.class,
                () -> RootCaCertificateParser.parsePemToCertificate("not a certificate"));
        assertThat(ex.getMessage(), containsString("no certificate was parsed"));
    }

    @Test
    public void parsePemToCertificate_rejectsMalformedPemEncoding() {
        String malformedPem = "-----BEGIN RSA PRIVATE KEY-----\nMIIBOgIBAAJBAK\n-----END RSA PRIVATE KEY-----";
        CertificateException ex = assertThrows(
                CertificateException.class,
                () -> RootCaCertificateParser.parsePemToCertificate(malformedPem));
        assertThat(ex.getMessage(), containsString("could not decode"));
    }
}
