package com.veertu.plugin.anka;

import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the identity-based "instances in progress" accounting that {@link AnkaMgmtCloud#provision}
 * uses to avoid starting duplicate VMs while earlier ones are still booting, connecting, or waiting
 * for a queued job to be dispatched.
 */
@WithJenkins
public class AnkaInstancesInProgressTest {

    @Test
    void tracksReservedNodesPerTemplate(JenkinsRule j) {
        StubAnkaCloud cloud = new StubAnkaCloud("in-progress-cloud-1");
        AnkaCloudSlaveTemplate template = cloud.onlyTemplate();

        assertEquals(0, cloud.countInstancesInProgress(template));

        cloud.reserveInstanceInProgress(template, "node-a");
        cloud.reserveInstanceInProgress(template, "node-b");
        assertEquals(2, cloud.countInstancesInProgress(template));

        cloud.releaseInstanceInProgress("node-a");
        assertEquals(1, cloud.countInstancesInProgress(template));

        cloud.releaseInstanceInProgress("node-b");
        assertEquals(0, cloud.countInstancesInProgress(template));
    }

    @Test
    void reserveIsIdempotentPerNodeName(JenkinsRule j) {
        StubAnkaCloud cloud = new StubAnkaCloud("in-progress-cloud-dupe");
        AnkaCloudSlaveTemplate template = cloud.onlyTemplate();

        cloud.reserveInstanceInProgress(template, "node-a");
        cloud.reserveInstanceInProgress(template, "node-a");
        assertEquals(1, cloud.countInstancesInProgress(template),
                "reserving the same node name twice must not double-count");
    }

    @Test
    void releaseIsIdempotentAndSafeForUnknownNodes(JenkinsRule j) {
        StubAnkaCloud cloud = new StubAnkaCloud("in-progress-cloud-2");
        AnkaCloudSlaveTemplate template = cloud.onlyTemplate();

        cloud.releaseInstanceInProgress("never-reserved");
        assertEquals(0, cloud.countInstancesInProgress(template));

        cloud.reserveInstanceInProgress(template, "node-a");
        cloud.releaseInstanceInProgress("node-a");
        cloud.releaseInstanceInProgress("node-a");
        assertEquals(0, cloud.countInstancesInProgress(template));
    }

    @Test
    void countsAreIsolatedPerCloud(JenkinsRule j) {
        StubAnkaCloud cloudA = new StubAnkaCloud("in-progress-cloud-A");
        StubAnkaCloud cloudB = new StubAnkaCloud("in-progress-cloud-B");

        cloudA.reserveInstanceInProgress(cloudA.onlyTemplate(), "node-a");

        assertEquals(1, cloudA.countInstancesInProgress(cloudA.onlyTemplate()));
        assertEquals(0, cloudB.countInstancesInProgress(cloudB.onlyTemplate()));
    }

    private static final class StubAnkaCloud extends AnkaMgmtCloud {
        StubAnkaCloud(String name) {
            super("https://stub-anka", name, "", "", true,
                    Collections.singletonList(template()), 0);
        }

        AnkaCloudSlaveTemplate onlyTemplate() {
            return getTemplates().get(0);
        }

        private static AnkaCloudSlaveTemplate template() {
            AnkaCloudSlaveTemplate t = new AnkaCloudSlaveTemplate();
            t.setLabelString("ephemeral-arm-mobile-client-bba-cache");
            t.setMasterVmId("00000000-0000-0000-0000-000000000001");
            return t;
        }
    }
}
