package com.veertu.ankaMgmtSdk;

import org.junit.Test;

import java.security.cert.CertificateException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThrows;

public class RootCaCertificateParserTest {

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
