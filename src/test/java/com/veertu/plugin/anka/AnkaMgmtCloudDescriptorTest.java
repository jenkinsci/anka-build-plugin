package com.veertu.plugin.anka;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.ItemGroup;
import hudson.util.ListBoxModel;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

public class AnkaMgmtCloudDescriptorTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void credentialsDropdownListsOnlyPluginAuthCredentialTypes() throws Exception {
        addCredential(new StringCredentialsImpl(
                CredentialsScope.GLOBAL,
                "secret-text-uak",
                "Secret text UAK",
                hudson.util.Secret.fromString("not-a-uak-key")));
        addCredential(new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL,
                "legacy-userpass",
                "Legacy username/password",
                "uak-id",
                "uak-secret"));
        addCredential(new CertCredentials(
                CredentialsScope.GLOBAL,
                "anka-mtls",
                "Production Controller",
                "Production Controller mTLS",
                "key",
                "cert"));
        addCredential(new AnkaUakTapCredentials(
                CredentialsScope.GLOBAL,
                "anka-uak",
                "Production UAK",
                "uak-id",
                "uak-secret"));

        AnkaMgmtCloud.DescriptorImpl descriptor = jenkinsRule.jenkins.getDescriptorByType(AnkaMgmtCloud.DescriptorImpl.class);
        ListBoxModel options = descriptor.doFillCredentialsIdItems(jenkinsRule.jenkins, null);

        List<String> values = options.stream()
                .map(option -> option.value)
                .collect(Collectors.toList());

        assertThat(values, containsInAnyOrder("", "anka-mtls", "anka-uak"));
        assertThat(values, not(containsInAnyOrder("secret-text-uak", "legacy-userpass")));
    }

    @Test
    public void credentialsDropdownPreservesLegacySelectedCredential() throws Exception {
        addCredential(new StringCredentialsImpl(
                CredentialsScope.GLOBAL,
                "secret-text-uak",
                "Secret text UAK",
                hudson.util.Secret.fromString("not-a-uak-key")));
        addCredential(new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL,
                "legacy-userpass",
                "Legacy username/password",
                "uak-id",
                "uak-secret"));

        AnkaMgmtCloud.DescriptorImpl descriptor = jenkinsRule.jenkins.getDescriptorByType(AnkaMgmtCloud.DescriptorImpl.class);

        ListBoxModel secretTextOptions = descriptor.doFillCredentialsIdItems(jenkinsRule.jenkins, "secret-text-uak");
        assertThat(
                secretTextOptions.stream().anyMatch(option -> "secret-text-uak".equals(option.value)),
                is(true));

        ListBoxModel userPassOptions = descriptor.doFillCredentialsIdItems(jenkinsRule.jenkins, "legacy-userpass");
        assertThat(
                userPassOptions.stream().anyMatch(option -> "legacy-userpass".equals(option.value)),
                is(true));
    }

    private void addCredential(com.cloudbees.plugins.credentials.Credentials credential) throws Exception {
        CredentialsProvider.lookupStores(jenkinsRule.jenkins)
                .iterator()
                .next()
                .addCredentials(Domain.global(), credential);
    }

    @Test
    public void rootCaCredentialsDropdownRequiresPost() throws Exception {
        Method method = AnkaMgmtCloud.DescriptorImpl.class.getMethod(
                "doFillRootCaCredentialsIdItems",
                ItemGroup.class);

        assertThat(method.isAnnotationPresent(RequirePOST.class), is(true));
    }

    @Test
    public void labelsApiTokenCredentialsDropdownRequiresPost() throws Exception {
        Method method = AnkaMgmtCloud.DescriptorImpl.class.getMethod(
                "doFillLabelsApiTokenCredentialsIdItems",
                ItemGroup.class);

        assertThat(method.isAnnotationPresent(RequirePOST.class), is(true));
    }

    @Test
    public void credentialsDropdownRequiresPost() throws Exception {
        Method method = AnkaMgmtCloud.DescriptorImpl.class.getMethod(
                "doFillCredentialsIdItems",
                ItemGroup.class,
                String.class);

        assertThat(method.isAnnotationPresent(RequirePOST.class), is(true));
    }
}
