package com.veertu.plugin.anka;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.util.Secret;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class AnkaLabelsApiTokenCredentials extends BaseStandardCredentials {

    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_LABEL = "Labels API Token";

    @NonNull
    private final Secret token;

    @DataBoundConstructor
    public AnkaLabelsApiTokenCredentials(
            @CheckForNull CredentialsScope scope,
            @CheckForNull String id,
            @CheckForNull String description,
            @CheckForNull String token) {
        super(
                scope,
                AnkaCredentialNaming.normalizeId(id, AnkaCredentialNaming.normalizeLabel(description, DEFAULT_LABEL)),
                AnkaCredentialNaming.normalizeLabel(description, DEFAULT_LABEL));
        this.token = Secret.fromString(Util.fixNull(token));
    }

    @NonNull
    public Secret getToken() {
        return token;
    }

    @Extension
    @Symbol("labelsApiToken")
    public static class DescriptorImpl extends BaseStandardCredentials.BaseStandardCredentialsDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return AnkaCredentialNaming.PREFIX + "Labels API Token";
        }
    }

    @Extension
    public static class NameProvider extends CredentialsNameProvider<AnkaLabelsApiTokenCredentials> {

        @Override
        public String getName(AnkaLabelsApiTokenCredentials credentials) {
            return AnkaCredentialNaming.displayLabel(credentials.getDescription(), DEFAULT_LABEL);
        }
    }
}
