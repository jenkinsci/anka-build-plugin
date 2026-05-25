package com.veertu.plugin.anka;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

public class AnkaUakTapCredentials extends BaseStandardCredentials implements StandardUsernamePasswordCredentials {

    private static final String DEFAULT_LABEL = "UAK/TAP Authentication";

    @NonNull
    private final String username;

    @NonNull
    private final Secret password;

    @DataBoundConstructor
    public AnkaUakTapCredentials(
            @CheckForNull CredentialsScope scope,
            @CheckForNull String id,
            @CheckForNull String description,
            @CheckForNull String username,
            @CheckForNull String password) {
        super(
                scope,
                AnkaCredentialNaming.normalizeId(id, AnkaCredentialNaming.normalizeLabel(description, DEFAULT_LABEL)),
                AnkaCredentialNaming.normalizeLabel(description, DEFAULT_LABEL));
        this.username = Util.fixNull(username);
        this.password = Secret.fromString(password);
    }

    @NonNull
    @Override
    public String getUsername() {
        return username;
    }

    @NonNull
    @Override
    public Secret getPassword() {
        return password;
    }

    @Override
    public boolean isUsernameSecret() {
        return false;
    }

    @Extension
    public static class DescriptorImpl extends BaseStandardCredentials.BaseStandardCredentialsDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return AnkaCredentialNaming.PREFIX + "UAK/TAP Authentication";
        }
    }

    @Extension
    public static class NameProvider extends CredentialsNameProvider<AnkaUakTapCredentials> {

        @Override
        public String getName(AnkaUakTapCredentials credentials) {
            return AnkaCredentialNaming.displayLabel(credentials.getDescription(), DEFAULT_LABEL);
        }
    }
}
