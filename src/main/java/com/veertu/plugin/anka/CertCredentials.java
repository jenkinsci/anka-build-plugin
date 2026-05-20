package com.veertu.plugin.anka;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;
import javax.annotation.CheckForNull;

public class CertCredentials implements Credentials, IdCredentials {

    private static final String DEFAULT_LABEL = "Certificate Authentication";

    @CheckForNull
    private final Secret clientKey;
    @CheckForNull
    private final String clientCertificate;
    private final String id;
    private final String name;
    private final CredentialsScope scope;
    private final String description;

    @DataBoundConstructor
    public CertCredentials(CredentialsScope scope, String id, String name, String description,
                                   @CheckForNull String clientKey, @CheckForNull String clientCertificate) {
        this.scope = scope;
        this.name = AnkaCredentialNaming.normalizeLabel(name, DEFAULT_LABEL);
        this.description = AnkaCredentialNaming.normalizeLabel(description, DEFAULT_LABEL);
        this.id = AnkaCredentialNaming.normalizeId(id, this.name);
        this.clientKey = Util.fixEmptyAndTrim(clientKey) == null ? null : Secret.fromString(clientKey);
        this.clientCertificate = Util.fixEmptyAndTrim(clientCertificate);
    }

    @Override
    @NonNull
    public String getId() {
        return this.id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @CheckForNull
    public String getClientKey() {
        return clientKey == null ? null : clientKey.getPlainText();
    }

    @CheckForNull
    public String getClientCertificate() {
        return clientCertificate;
    }

    @Override
    public CredentialsScope getScope() {
        return scope;
    }

    @NonNull
    @Override
    public CredentialsDescriptor getDescriptor() {
        return new CertCredentials.DescriptorImpl();
    }


    @Extension
    public static class DescriptorImpl extends CredentialsDescriptor {
        @Override
        public String getDisplayName() {
            return AnkaCredentialNaming.PREFIX + "Certificate Authentication";
        }
    }

    @Extension
    public static class NameProvider extends CredentialsNameProvider<CertCredentials> {

        @Override
        public String getName(CertCredentials credentials) {
            return AnkaCredentialNaming.displayLabel(credentials.getName(), DEFAULT_LABEL);
        }
    }

}