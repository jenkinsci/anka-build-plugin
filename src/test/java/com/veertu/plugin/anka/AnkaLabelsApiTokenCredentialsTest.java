package com.veertu.plugin.anka;

import com.cloudbees.plugins.credentials.CredentialsScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class AnkaLabelsApiTokenCredentialsTest {

    @Test
    public void normalizesBlankIdFromDescription() {
        AnkaLabelsApiTokenCredentials credentials = new AnkaLabelsApiTokenCredentials(
                CredentialsScope.GLOBAL,
                null,
                null,
                "secret-token");

        assertThat(credentials.getId(), is("anka-build-cloud-plugin-labels-api-token"));
        assertThat(credentials.getDescription(), is(AnkaCredentialNaming.PREFIX + "Labels API Token"));
        assertThat(credentials.getToken().getPlainText(), is("secret-token"));
    }

    @Test
    public void preservesConfiguredId() {
        AnkaLabelsApiTokenCredentials credentials = new AnkaLabelsApiTokenCredentials(
                CredentialsScope.GLOBAL,
                "anka-labels-api",
                AnkaCredentialNaming.PREFIX + "Production Labels API",
                "secret-token");

        assertThat(credentials.getId(), is("anka-labels-api"));
        assertThat(credentials.getDescription(), is(AnkaCredentialNaming.PREFIX + "Production Labels API"));
    }
}
