package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import hudson.init.InitMilestone;
import hudson.model.Computer;
import hudson.model.Label;
import hudson.model.Node;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * During Jenkins init, {@code isAlive}/{@code isSchedulingOrPulling}/{@code isConnecting} and
 * run-once {@code start()} must not call the Anka controller on the caller thread. Jenkins core's
 * {@link hudson.model.Computer} {@code isAlive()}, {@link AnkaMgmtCloud#canProvision} and
 * {@link AnkaCloudComputer#onRemoved} must never call the controller on the caller thread.
 */
@WithJenkins
public class AnkaStartupControllerGuardTest {

    private JenkinsRule jenkinsRule;

    @BeforeEach
    void setUp(JenkinsRule jenkinsRule) {
        this.jenkinsRule = jenkinsRule;
    }

    @AfterEach
    void clearForcedInitLevel() {
        AnkaJenkinsInit.clearForcedInitLevelForTests();
    }

    @Test
    public void isAliveSkipsControllerWhileJenkinsIsStarting() throws Exception {
        AnkaMgmtCloud cloud = mock(AnkaMgmtCloud.class);
        when(cloud.showInstance(anyString())).thenThrow(new AnkaMgmtException("must not be called"));
        AbstractAnkaSlave slave = newSlave(cloud, "startup-alive", "vm-alive");

        AnkaJenkinsInit.forceInitLevelForTests(InitMilestone.STARTED);

        assertTrue(slave.isAlive());
        verifyNoInteractions(cloud);
    }

    @Test
    public void isSchedulingOrPullingSkipsControllerWhileJenkinsIsStarting() throws Exception {
        AnkaMgmtCloud cloud = mock(AnkaMgmtCloud.class);
        when(cloud.showInstance(anyString())).thenThrow(new AnkaMgmtException("must not be called"));
        AbstractAnkaSlave slave = newSlave(cloud, "startup-sched", "vm-sched");

        AnkaJenkinsInit.forceInitLevelForTests(InitMilestone.JOB_LOADED);

        assertFalse(slave.isSchedulingOrPulling());
        verifyNoInteractions(cloud);
    }

    @Test
    public void isConnectingSkipsControllerWhileJenkinsIsStarting() throws Exception {
        AnkaMgmtCloud cloud = mock(AnkaMgmtCloud.class);
        when(cloud.showInstance(anyString())).thenThrow(new AnkaMgmtException("must not be called"));
        AbstractAnkaSlave slave = newSlave(cloud, "startup-conn", "vm-conn");
        AnkaCloudComputer computer = new AnkaCloudComputer(slave, "vm-conn");

        AnkaJenkinsInit.forceInitLevelForTests(InitMilestone.PLUGINS_STARTED);

        computer.isConnecting();
        verifyNoInteractions(cloud);
    }

    @Test
    public void constructedSlaveIsNotLoadedFromDiskButDeserializedSlaveIs() throws Exception {
        AnkaMgmtCloud cloud = registerBlackholeCloud();
        AbstractAnkaSlave slave = newSlave(cloud, "disk-flag", "vm-disk");
        assertFalse(slave.wasLoadedFromDisk());

        AbstractAnkaSlave restored = (AbstractAnkaSlave) Jenkins.XSTREAM2.fromXML(Jenkins.XSTREAM2.toXML(slave));
        assertTrue(restored.wasLoadedFromDisk());
    }

    @Test
    public void coreComputerIsAliveDoesNotCallController() throws Exception {
        AnkaMgmtCloud cloud = mock(AnkaMgmtCloud.class);
        when(cloud.showInstance(anyString())).thenThrow(new AnkaMgmtException("must not be called"));
        AbstractAnkaSlave slave = newSlave(cloud, "core-alive", "vm-core");
        AnkaCloudComputer computer = spy(new AnkaCloudComputer(slave, "vm-core"));
        doReturn(slave).when(computer).getNode();
        Method coreIsAlive = Computer.class.getDeclaredMethod("isAlive");
        coreIsAlive.setAccessible(true);

        coreIsAlive.invoke(computer);
        verifyNoInteractions(cloud);
    }

    @Test
    public void startReconnectsNodeLoadedFromDiskInBackgroundAfterInitCompleted() throws Exception {
        // Jenkins core runs updateComputerList() after COMPLETED is already set.
        AnkaMgmtCloud cloud = registerBlackholeCloud();
        AbstractAnkaSlave loadedNode = mock(AbstractAnkaSlave.class);
        when(loadedNode.wasLoadedFromDisk()).thenReturn(true);
        AnkaCloudComputer computer = mockComputerFor(loadedNode);
        List<Object[]> captured = new ArrayList<>();

        assertTrue(AnkaJenkinsInit.isStartupComplete());
        capturingStrategy(captured).start(computer);

        assertEquals(1, captured.size());
        assertSame(computer, captured.get(0)[0]);
        assertSame(cloud, captured.get(0)[1]);
        verify(computer, never()).connect(false);
        verify(computer, never()).firstConnectionAttempted();
    }

    @Test
    public void startConnectsNewlyProvisionedNodeOnCallerThread() throws Exception {
        registerBlackholeCloud();
        AbstractAnkaSlave newNode = mock(AbstractAnkaSlave.class);
        when(newNode.wasLoadedFromDisk()).thenReturn(false);
        AnkaCloudComputer computer = mockComputerFor(newNode);
        List<Object[]> captured = new ArrayList<>();

        capturingStrategy(captured).start(computer);

        assertTrue(captured.isEmpty());
        verify(computer).connect(false);
        verify(computer).firstConnectionAttempted();
    }

    @Test
    public void checkSkipsControllerForNodeLoadedFromDiskUntilBackgroundCheckFinishes() {
        int idleMinutes = 5;
        assertEquals(idleMinutes, checkWhileControllerReportsScheduling(true, idleMinutes));
        assertEquals(idleMinutes * 3, checkWhileControllerReportsScheduling(false, idleMinutes));
    }

    @Test
    public void isConnectingSkipsControllerForNodeLoadedFromDiskUntilBackgroundCheckFinishes() throws Exception {
        AnkaMgmtCloud cloud = mock(AnkaMgmtCloud.class);
        when(cloud.showInstance(anyString())).thenThrow(new AnkaMgmtException("must not be called"));
        AbstractAnkaSlave slave = spy(newSlave(cloud, "disk-conn", "vm-disk-conn"));
        doReturn(true).when(slave).wasLoadedFromDisk();
        AnkaCloudComputer computer = spy(new AnkaCloudComputer(slave, "vm-disk-conn"));
        doReturn(slave).when(computer).getNode();

        assertTrue(AnkaJenkinsInit.isStartupComplete());
        assertTrue(computer.awaitingBackgroundRestartCheck());
        computer.isConnecting();
        verifyNoInteractions(cloud);

        computer.firstConnectionAttempted();
        assertFalse(computer.awaitingBackgroundRestartCheck());
    }

    @Test
    public void canProvisionDoesNotCallControllerForAutomaticCapacity() {
        // Jenkins core runs Nodes.load -> trimLabels -> canProvision under the Queue lock at startup.
        String labelName = "blackhole-capacity";
        AnkaMgmtCloud cloud = registerBlackholeCloud(List.of(labeledTemplate(labelName)), 0);

        assertTimeoutPreemptively(Duration.ofSeconds(10),
                () -> assertTrue(cloud.canProvision(new Cloud.CloudState(Label.get(labelName), 0))));
    }

    @Test
    public void canProvisionKeepsConfiguredCloudCapacity() {
        int configuredCloudCapacity = 3;
        AnkaMgmtCloud cloud = registerBlackholeCloud(Collections.emptyList(), configuredCloudCapacity);

        assertEquals(configuredCloudCapacity, cloud.getCloudCapacityWithoutController());
    }

    @Test
    public void onRemovedDoesNotCallControllerOnCallerThread() throws Exception {
        // Jenkins core calls onRemoved under the Queue lock (AbstractCIBase.removeComputer).
        AnkaMgmtCloud cloud = registerBlackholeCloud();
        AnkaCloudComputer computer = new AnkaCloudComputer(newSlave(cloud, "removed-node", "vm-removed"), "vm-removed");

        assertTimeoutPreemptively(Duration.ofSeconds(10), computer::onRemoved);
    }

    @Test
    public void terminateVMInstanceInBackgroundTerminatesOnExecutor() throws Exception {
        AnkaMgmtCloud cloud = mock(AnkaMgmtCloud.class);
        AnkaCloudComputer computer = new AnkaCloudComputer(newSlave(cloud, "removed-bg", "vm-removed-bg"), "vm-removed-bg");
        List<Runnable> submittedTasks = new ArrayList<>();

        computer.terminateVMInstanceInBackground(cloud, submittedTasks::add);
        verifyNoInteractions(cloud);
        assertEquals(1, submittedTasks.size());

        submittedTasks.get(0).run();
        verify(cloud).terminateVMInstance("vm-removed-bg");
    }

    private static AnkaCloudSlaveTemplate labeledTemplate(String labelName) {
        AnkaCloudSlaveTemplate template = new AnkaCloudSlaveTemplate();
        template.setLabelString(labelName);
        template.setLabelSet(Label.parse(labelName));
        template.setMode(Node.Mode.EXCLUSIVE);
        template.setMasterVmId("00000000-0000-0000-0000-000000000001");
        return template;
    }

    private static long checkWhileControllerReportsScheduling(boolean awaitingBackgroundRestartCheck, int idleMinutes) {
        AnkaCloudComputer computer = mockComputerFor(mock(AbstractAnkaSlave.class));
        when(computer.awaitingBackgroundRestartCheck()).thenReturn(awaitingBackgroundRestartCheck);
        when(computer.isSchedulingOrPulling()).thenReturn(true);
        return new RunOnceCloudRetentionStrategy(idleMinutes).check(computer);
    }

    private AnkaMgmtCloud registerBlackholeCloud() {
        return registerBlackholeCloud(Collections.emptyList(), 0);
    }

    private AnkaMgmtCloud registerBlackholeCloud(List<AnkaCloudSlaveTemplate> templates, int cloudInstanceCap) {
        AnkaMgmtCloud cloud = new AnkaMgmtCloud(
                "http://192.0.2.1:80",
                "anka-startup-guard",
                null,
                null,
                true,
                templates,
                cloudInstanceCap);
        jenkinsRule.jenkins.clouds.add(cloud);
        return cloud;
    }

    private static AnkaCloudComputer mockComputerFor(AbstractAnkaSlave node) {
        AnkaCloudComputer computer = mock(AnkaCloudComputer.class);
        when(computer.getName()).thenReturn("startup-reconnect");
        when(computer.getCloudName()).thenReturn("anka-startup-guard");
        when(computer.getVMId()).thenReturn("vm-1");
        when(computer.getNode()).thenReturn(node);
        return computer;
    }

    private static RunOnceCloudRetentionStrategy capturingStrategy(List<Object[]> captured) {
        return new RunOnceCloudRetentionStrategy(1) {
            @Override
            void reconnectAfterRestartInBackground(AnkaCloudComputer c, AnkaMgmtCloud cl, Executor executor) {
                captured.add(new Object[]{c, cl});
            }
        };
    }

    private static AbstractAnkaSlave newSlave(AnkaMgmtCloud cloud, String nodeName, String vmId) throws Exception {
        AnkaCloudSlaveTemplate template = new AnkaCloudSlaveTemplate();
        template.setCloudName("anka-startup-guard");
        template.setRemoteFS("/tmp");
        template.setLaunchMethod(LaunchMethod.SSH);
        template.setRetentionStrategy(new RunOnceCloudRetentionStrategy(5));
        return new AnkaOnDemandSlave(
                cloud,
                nodeName,
                "desc",
                "/tmp",
                1,
                Node.Mode.NORMAL,
                "label",
                new AnkaLauncher(cloud, template, vmId),
                Collections.emptyList(),
                template,
                vmId);
    }
}
