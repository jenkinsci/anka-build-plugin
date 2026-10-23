package com.veertu.plugin.anka;

import hudson.init.InitMilestone;
import jenkins.model.Jenkins;

/**
 * Init-thread guards for slave methods that still run while the reactor is below
 * {@link InitMilestone#COMPLETED}. Note: {@code RetentionStrategy.start()} can also run
 * after COMPLETED on the startup thread (see {@link RunOnceCloudRetentionStrategy#start}).
 */
final class AnkaJenkinsInit {

    /** Test-only override; {@code null} means use the live Jenkins init level. */
    private static volatile InitMilestone forcedInitLevelForTests;

    private AnkaJenkinsInit() {
    }

    static void forceInitLevelForTests(InitMilestone level) {
        forcedInitLevelForTests = level;
    }

    static void clearForcedInitLevelForTests() {
        forcedInitLevelForTests = null;
    }

    static boolean isStartupComplete() {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins == null) {
            return false;
        }
        InitMilestone forced = forcedInitLevelForTests;
        InitMilestone level = forced != null ? forced : jenkins.getInitLevel();
        return isStartupComplete(level);
    }

    static boolean isStartupComplete(InitMilestone level) {
        return level == InitMilestone.COMPLETED;
    }
}
