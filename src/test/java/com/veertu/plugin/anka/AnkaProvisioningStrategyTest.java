package com.veertu.plugin.anka;

import hudson.ExtensionList;
import hudson.model.Label;
import hudson.model.queue.QueueListener;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@WithJenkins
public class AnkaProvisioningStrategyTest {

    private static final String LABEL_NAME = "anka-fast-provision";

    private JenkinsRule jenkinsRule;

    @BeforeEach
    void setUp(JenkinsRule jenkinsRule) {
        this.jenkinsRule = jenkinsRule;
    }

    @Test
    public void shouldRegisterFastProvisioningQueueListener() {
        AnkaProvisioningStrategy.FastProvisioning listener =
                ExtensionList.lookup(QueueListener.class).get(AnkaProvisioningStrategy.FastProvisioning.class);

        assertThat(listener, notNullValue());
    }

    @Test
    public void shouldSuggestProvisionerReviewWhenAnkaCloudCanProvision() throws Exception {
        Label label = Label.get(LABEL_NAME);
        NodeProvisioner provisioner = label.nodeProvisioner;
        jenkinsRule.jenkins.clouds.add(stubAnkaCloud(true));
        long lastReviewBefore = getLastSuggestedReview(provisioner);

        AnkaProvisioningStrategy.suggestReviewForBuildableLabel(jenkinsRule.jenkins, label);

        assertThat(getLastSuggestedReview(provisioner) > lastReviewBefore
                || isProvisionerReviewQueued(provisioner), is(true));
    }

    @Test
    public void shouldNotSuggestProvisionerReviewWhenAnkaCloudCannotProvision() throws Exception {
        Label label = Label.get(LABEL_NAME);
        NodeProvisioner provisioner = label.nodeProvisioner;
        jenkinsRule.jenkins.clouds.add(stubAnkaCloud(false));
        long lastReviewBefore = getLastSuggestedReview(provisioner);
        boolean queuedReviewBefore = isProvisionerReviewQueued(provisioner);

        AnkaProvisioningStrategy.suggestReviewForBuildableLabel(jenkinsRule.jenkins, label);

        assertThat(getLastSuggestedReview(provisioner), is(lastReviewBefore));
        assertThat(isProvisionerReviewQueued(provisioner), is(queuedReviewBefore));
    }

    @Test
    public void shouldNotSuggestProvisionerReviewForNonAnkaCloud() throws Exception {
        Label label = Label.get(LABEL_NAME);
        NodeProvisioner provisioner = label.nodeProvisioner;
        jenkinsRule.jenkins.clouds.add(new OtherCloud());
        long lastReviewBefore = getLastSuggestedReview(provisioner);
        boolean queuedReviewBefore = isProvisionerReviewQueued(provisioner);

        AnkaProvisioningStrategy.suggestReviewForBuildableLabel(jenkinsRule.jenkins, label);

        assertThat(getLastSuggestedReview(provisioner), is(lastReviewBefore));
        assertThat(isProvisionerReviewQueued(provisioner), is(queuedReviewBefore));
    }

    private static AnkaMgmtCloud stubAnkaCloud(final boolean canProvision) {
        AnkaCloudSlaveTemplate template = new AnkaCloudSlaveTemplate();
        template.setLabelString(LABEL_NAME);
        template.setMasterVmId("00000000-0000-0000-0000-000000000001");

        return new AnkaMgmtCloud(
                "https://stub-anka",
                "stub-cloud",
                "",
                "",
                true,
                Collections.singletonList(template),
                0) {
            @Override
            public boolean canProvision(CloudState state) {
                return canProvision;
            }
        };
    }

    private static boolean isProvisionerReviewQueued(NodeProvisioner provisioner) throws Exception {
        Field queuedReviewField = NodeProvisioner.class.getDeclaredField("queuedReview");
        queuedReviewField.setAccessible(true);
        return queuedReviewField.getBoolean(provisioner);
    }

    private static long getLastSuggestedReview(NodeProvisioner provisioner) throws Exception {
        Field lastSuggestedReviewField = NodeProvisioner.class.getDeclaredField("lastSuggestedReview");
        lastSuggestedReviewField.setAccessible(true);
        return lastSuggestedReviewField.getLong(provisioner);
    }

    private static class OtherCloud extends Cloud {
        OtherCloud() {
            super("other-cloud");
        }

        @Override
        public boolean canProvision(CloudState state) {
            return true;
        }
    }
}
