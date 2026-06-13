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

    private static final String SECOND_CERTIFICATE = """
            -----BEGIN CERTIFICATE-----
            MIIDJzCCAg+gAwIBAgIUHdi0mjZCogldRA9wblQa29ipuMYwDQYJKoZIhvcNAQEL
            BQAwIzEhMB8GA1UEAwwYQW5rYSBSb290IENBIFBhcnNlciBUZXN0MB4XDTI2MDYx
            MjE0MjAwOVoXDTM2MDYwOTE0MjAwOVowIzEhMB8GA1UEAwwYQW5rYSBSb290IENB
            IFBhcnNlciBUZXN0MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqnpf
            Q1O7kN9kcOLlS7GYHoKMcNrog1scCmaYRcsTLcpiqNhbe46q/S7cKC0Jef/kcHP1
            r9lhwyNgemKn462wMws9tJok7D8Lf+6ICNXQzbbv4p2BiNiQb6BGhs3mVnM1FS4m
            pFpQEFEOLHhE/II5cAR2Ct6mY3EXQKRyt+m8AUxt6l5Ye8EuiDWTmc00lWCrWIg4
            SIPFIHTzz6bEYcPJYOtRPhQTUsl6kboGzEWY/967aMn6mUgioIl+n7QC9WyhvMep
            hmtV935gtiDTt18hURgTB7N5qxMZRGW/XcZ6iasFA+Eq+juPFf9BdecD2Nnr3h0w
            ipS7a+S4ivQk9EF1LwIDAQABo1MwUTAdBgNVHQ4EFgQUv2hcL2t2z2H7fJLLq96n
            mjQBc5IwHwYDVR0jBBgwFoAUv2hcL2t2z2H7fJLLq96nmjQBc5IwDwYDVR0TAQH/
            BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAb4wG4EHfO+uhFKilqKI5v2Cpaob1
            kV/Dn3SRzZa90ZxZzVedPH/2Or+uFRrPqFddp0m/HySsZ/1JpMFlznr7TgWyAzff
            +pwqPvG/UQLULyazuNj4unVJfhO8u2luXOxs0yk73oh0L9Bo1ElqBLeWy4TLoxxK
            dRvSj1J0lJ59u7wkP9Q6Wo+Euxz0XlqMNrCjeXKBHouQPM+x/1o7yk0cAc+sU6G9
            k4cdttFWV1Kun9PLI/2n3qgQT2VdsW26km6+3OYDw/5w97QI6XEMm3yxNQliJP5Y
            3TEI6HNBplSFlB+RoZG8mcGHT1pz+JwIdkKJ+nosKOGvTIGMJ9eyHI4Qkw==
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
    public void parsePemToCertificate_acceptsCertificateBundle() throws Exception {
        assertNotNull(RootCaCertificateParser.parsePemToCertificate(
                VALID_CERTIFICATE + SECOND_CERTIFICATE));
    }

    @Test
    public void parsePemToCertificate_acceptsPemWithSurroundingMetadata() throws Exception {
        String withMetadata = "subject=/CN=Anka Root CA Parser Test\n"
                + "issuer=/CN=Anka Root CA Parser Test\n"
                + VALID_CERTIFICATE
                + "\ntrailing metadata that openssl x509 -text may append\n";
        assertNotNull(RootCaCertificateParser.parsePemToCertificate(withMetadata));
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
