package com.veertu.plugin.anka;

import com.cloudbees.plugins.credentials.CredentialsScope;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class AnkaMgmtCloudCredentialContextTest {

    @Test
    public void describeCredentialFormatsCertCredentials() {
        CertCredentials credentials = new CertCredentials(
                CredentialsScope.GLOBAL,
                "safd",
                "Anka Build Cloud Plugin: Certificate Authentication",
                "Anka Build Cloud Plugin: Certificate Authentication",
                "key",
                "cert");

        assertThat(
                AnkaMgmtCloud.describeCredential(credentials, "safd"),
                is("mTLS id=safd name=Anka Build Cloud Plugin: Certificate Authentication"));
    }

    @Test
    public void describeCredentialFormatsMissingCredentialById() {
        assertThat(
                AnkaMgmtCloud.describeCredential(null, "missing-id"),
                is("configured id=missing-id (not found in credential store)"));
    }
}
