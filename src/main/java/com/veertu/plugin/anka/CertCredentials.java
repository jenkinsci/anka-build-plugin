package com.veertu.plugin.anka;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;
import javax.annotation.CheckForNull;

public class CertCredentials implements Credentials {

    @CheckForNull
    private final Secret clientKey;
    @CheckForNull
    private final String clientCertificate;
    private final String id;
    private final String name;
    private final CredentialsScope scope;

    @DataBoundConstructor
    public CertCredentials(CredentialsScope scope, String id, String name, String description,
                                   @CheckForNull String clientKey, @CheckForNull String clientCertificate) {
        this.id = IdCredentials.Helpers.fixEmptyId(id);
        this.scope = scope;
        this.name = name;
        this.clientKey = Util.fixEmptyAndTrim(clientKey) == null ? null : Secret.fromString(clientKey);
        this.clientCertificate = Util.fixEmptyAndTrim(clientCertificate);
    }

    @NonNull
    public String getId() {
        return this.id;
    }

    public String getName() {
        return name;
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
            return "Certificate Authentication";
        }
    }

}