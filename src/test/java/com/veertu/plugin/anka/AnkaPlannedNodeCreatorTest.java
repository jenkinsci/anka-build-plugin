package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaVmInfo;
import com.veertu.ankaMgmtSdk.AnkaVmInstance;
import hudson.model.Node;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reproduction tests for the over-provisioning / orphaned-VM regression seen on plugin
 * 2.16.x and 3.x.
 *
 * <p>During a burst of concurrent builds the Anka controller can transiently fail to
 * return a freshly-started instance from {@code showInstance}. {@link AnkaPlannedNodeCreator#waitAndConnect}
 * treats that single miss as a fatal "not found" and abandons the provisioning attempt
 * (returns {@code null}) without ever calling {@link AnkaCloudComputer#firstConnectionAttempted()}.
 * The VM, however, keeps booting and its inbound agent connects on its own, producing a
 * node that never ran a job and that {@link RunOnceCloudRetentionStrategy#check} refuses to
 * reap because {@code afterFirstConnection()} stays {@code false}.
 *
 * <p>Both tests below FAIL against the current code (which returns on the first miss) and
 * PASS once a transient lookup miss is retried instead of abandoned.
 */
public class AnkaPlannedNodeCreatorTest {

    private static final String INSTANCE_ID = "vm-transient-1";

    @Test
    public void transientInstanceLookupMissMustNotAbandonProvisioning() throws Exception {
        Fixture fixture = new Fixture();

        Node result = AnkaPlannedNodeCreator.waitAndConnect(fixture.cloud, fixture.template, fixture.slave);

        assertThat("A single transient controller miss must not abandon provisioning; the "
                + "started instance should be picked up on retry.", result, sameInstance(fixture.slave));
        // Proves the code looked again after the first null instead of giving up immediately.
        verify(fixture.cloud, atLeast(2)).showInstance(INSTANCE_ID);
    }

    @Test
    public void transientInstanceLookupMissMustStillMarkFirstConnection() throws Exception {
        Fixture fixture = new Fixture();

        AnkaPlannedNodeCreator.waitAndConnect(fixture.cloud, fixture.template, fixture.slave);

        // Without this flag being set, RunOnceCloudRetentionStrategy.check() short-circuits at
        // its `!afterFirstConnection()` guard forever and the idle node is never terminated.
        verify(fixture.computer).firstConnectionAttempted();
    }

    @Test
    public void markFirstConnectionAttemptedIsNoOpWhenComputerIsNull() throws Exception {
        Fixture fixture = new Fixture();
        when(fixture.slave.toComputer()).thenReturn(null);

        Node result = AnkaPlannedNodeCreator.waitAndConnect(fixture.cloud, fixture.template, fixture.slave);

        assertThat(result, sameInstance(fixture.slave));
        verify(fixture.computer, never()).firstConnectionAttempted();
    }

    @Test
    public void persistentInstanceLookupMissMustAbandonProvisioningAfterMaxRetries() throws Exception {
        AnkaMgmtCloud cloud = mock(AnkaMgmtCloud.class);
        AnkaCloudSlaveTemplate template = mock(AnkaCloudSlaveTemplate.class);
        AbstractAnkaSlave slave = mock(AbstractAnkaSlave.class);
        AnkaCloudComputer computer = mock(AnkaCloudComputer.class);

        when(slave.getInstanceId()).thenReturn(INSTANCE_ID);
        when(slave.toComputer()).thenReturn(computer);
        when(cloud.getVmPollTime()).thenReturn(1);
        when(cloud.showInstance(INSTANCE_ID)).thenReturn(null);

        Node result = AnkaPlannedNodeCreator.waitAndConnect(cloud, template, slave);

        assertThat(result, is(nullValue()));
        verify(cloud, times(10)).showInstance(INSTANCE_ID);
        verify(computer).firstConnectionAttempted();
        verify(cloud).terminateVMInstance(INSTANCE_ID);
    }

    /**
     * Simulates one transient {@code showInstance} miss followed by a healthy Started
     * instance with an assigned host IP.
     */
    private static final class Fixture {
        private final AnkaMgmtCloud cloud = mock(AnkaMgmtCloud.class);
        private final AnkaCloudSlaveTemplate template = mock(AnkaCloudSlaveTemplate.class);
        private final AbstractAnkaSlave slave = mock(AbstractAnkaSlave.class);
        private final AnkaCloudComputer computer = mock(AnkaCloudComputer.class);
        private final AnkaVmInstance startedInstance = mock(AnkaVmInstance.class);
        private final AnkaVmInfo vmInfo = mock(AnkaVmInfo.class);

        private Fixture() throws Exception {
            when(slave.getInstanceId()).thenReturn(INSTANCE_ID);
            when(slave.toComputer()).thenReturn(computer);
            when(cloud.getVmPollTime()).thenReturn(1);
            // First lookup misses (controller under burst load), second returns the VM.
            when(cloud.showInstance(INSTANCE_ID)).thenReturn(null, startedInstance);
            when(startedInstance.isStarted()).thenReturn(true);
            when(startedInstance.getVmInfo()).thenReturn(vmInfo);
            when(vmInfo.getHostIp()).thenReturn("10.0.0.5");
        }
    }
}
