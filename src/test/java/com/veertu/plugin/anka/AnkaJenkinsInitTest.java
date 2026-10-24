package com.veertu.plugin.anka;

import hudson.init.InitMilestone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AnkaJenkinsInitTest {

    @AfterEach
    void clearForcedInitLevel() {
        AnkaJenkinsInit.clearForcedInitLevelForTests();
    }

    @Test
    public void startupIsCompleteOnlyAtCompletedMilestone() {
        assertFalse(AnkaJenkinsInit.isStartupComplete(InitMilestone.STARTED));
        assertFalse(AnkaJenkinsInit.isStartupComplete(InitMilestone.PLUGINS_STARTED));
        assertFalse(AnkaJenkinsInit.isStartupComplete(InitMilestone.SYSTEM_CONFIG_LOADED));
        assertFalse(AnkaJenkinsInit.isStartupComplete(InitMilestone.JOB_LOADED));
        assertTrue(AnkaJenkinsInit.isStartupComplete(InitMilestone.COMPLETED));
    }

    @Test
    public void noArgReturnsFalseWhenJenkinsIsAbsent() {
        assertFalse(AnkaJenkinsInit.isStartupComplete());
    }

    @Test
    @WithJenkins
    public void noArgUsesLiveJenkinsInitLevelWhenComplete(JenkinsRule j) {
        assertTrue(AnkaJenkinsInit.isStartupComplete());
    }

    @Test
    @WithJenkins
    public void forcedInitLevelOverridesLiveJenkins(JenkinsRule j) {
        AnkaJenkinsInit.forceInitLevelForTests(InitMilestone.STARTED);
        assertFalse(AnkaJenkinsInit.isStartupComplete());

        AnkaJenkinsInit.forceInitLevelForTests(InitMilestone.COMPLETED);
        assertTrue(AnkaJenkinsInit.isStartupComplete());
    }
}
