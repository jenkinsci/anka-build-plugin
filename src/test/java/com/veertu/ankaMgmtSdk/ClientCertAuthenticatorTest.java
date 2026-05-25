package com.veertu.ankaMgmtSdk;

import org.junit.Test;

import java.security.cert.CertificateException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThrows;

public class ClientCertAuthenticatorTest {

    @Test
    public void parseClientCertificate_rejectsNonPemText() {
        CertificateException ex = assertThrows(
                CertificateException.class,
                () -> ClientCertAuthenticator.parseClientCertificate("not a certificate"));
        assertThat(ex.getMessage(), containsString("no certificate was parsed"));
    }

    @Test
    public void parseClientPrivateKey_rejectsNonPemText() {
        CertificateException ex = assertThrows(
                CertificateException.class,
                () -> ClientCertAuthenticator.parseClientPrivateKey("not a private key"));
        assertThat(ex.getMessage(), containsString("no private key was parsed"));
    }
}
