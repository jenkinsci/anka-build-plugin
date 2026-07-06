package com.veertu.plugin.anka;

import hudson.model.Label;
import hudson.model.LoadStatistics;
import hudson.model.Node;
import hudson.slaves.Cloud;
import hudson.slaves.CloudProvisioningListener;
import hudson.slaves.NodeProvisioner;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AnkaProvisioningStrategy}, the fast one-shot provisioning strategy.
 */
@WithJenkins
public class AnkaProvisioningStrategyTest {

    private static final String LABEL_NAME = "ephemeral-arm-mobile-client-bba-cache";

    /**
     * Because the custom strategy calls {@link Cloud#provision} directly (bypassing
     * {@code NodeProvisioner}'s built-in {@code fireOnStarted}), it must fire
     * {@link CloudProvisioningListener#onStarted} itself so cloud-stats and friends can track the
     * nodes.
     */
    @Test
    void firesOnStartedForProvisionedNodes(JenkinsRule j) {
        Label label = Label.get(LABEL_NAME);
        List<NodeProvisioner.PlannedNode> planned = List.of(plannedNode("a"), plannedNode("b"));
        j.jenkins.clouds.add(new FakeAnkaCloud(planned));
        RecordingProvisioningListener.started.clear();

        NodeProvisioner.StrategyState state = strategyState(label, 0, 0, 0, planned.size());
        NodeProvisioner.StrategyDecision decision = new AnkaProvisioningStrategy().apply(state);

        assertEquals(planned, new ArrayList<>(RecordingProvisioningListener.started),
                "strategy must fire CloudProvisioningListener.onStarted for provisioned nodes");
        verify(state).recordPendingLaunches(planned);
        assertEquals(NodeProvisioner.StrategyDecision.PROVISIONING_COMPLETED, decision);
    }

    /**
     * When existing/planned capacity already meets demand the strategy must not provision anything.
     */
    @Test
    void doesNotProvisionWhenCapacityMeetsDemand(JenkinsRule j) {
        Label label = Label.get(LABEL_NAME);
        FakeAnkaCloud cloud = new FakeAnkaCloud(List.of(plannedNode("a")));
        j.jenkins.clouds.add(cloud);

        // demand 3, and available+connecting+planned = 1+1+1 = 3 -> nothing to do
        NodeProvisioner.StrategyState state = strategyState(label, 1, 1, 1, 3);
        NodeProvisioner.StrategyDecision decision = new AnkaProvisioningStrategy().apply(state);

        assertEquals(0, cloud.provisionCalls, "must not call provision when capacity already meets demand");
        assertEquals(NodeProvisioner.StrategyDecision.PROVISIONING_COMPLETED, decision);
    }

    private static NodeProvisioner.PlannedNode plannedNode(String name) {
        return new NodeProvisioner.PlannedNode(name, new CompletableFuture<Node>(), 1);
    }

    private NodeProvisioner.StrategyState strategyState(Label label, int available, int connecting,
                                                        int planned, int queueLength) {
        LoadStatistics.LoadStatisticsSnapshot snap = mock(LoadStatistics.LoadStatisticsSnapshot.class);
        when(snap.getAvailableExecutors()).thenReturn(available);
        when(snap.getConnectingExecutors()).thenReturn(connecting);
        when(snap.getQueueLength()).thenReturn(queueLength);

        NodeProvisioner.StrategyState state = mock(NodeProvisioner.StrategyState.class);
        when(state.getLabel()).thenReturn(label);
        when(state.getSnapshot()).thenReturn(snap);
        when(state.getPlannedCapacitySnapshot()).thenReturn(planned);
        when(state.getAdditionalPlannedCapacity()).thenReturn(0);
        return state;
    }

    /** Minimal Anka cloud that returns a fixed set of planned nodes and counts provision() calls. */
    private static final class FakeAnkaCloud extends AnkaMgmtCloud {
        private final transient Collection<NodeProvisioner.PlannedNode> planned;
        private transient int provisionCalls;

        FakeAnkaCloud(Collection<NodeProvisioner.PlannedNode> planned) {
            super("https://stub-anka", "stub-strategy", "", "", true,
                    Collections.singletonList(template()), 0);
            this.planned = planned;
        }

        private static AnkaCloudSlaveTemplate template() {
            AnkaCloudSlaveTemplate t = new AnkaCloudSlaveTemplate();
            t.setLabelString(LABEL_NAME);
            t.setMasterVmId("00000000-0000-0000-0000-000000000001");
            return t;
        }

        @Override
        public boolean canProvision(CloudState state) {
            return true;
        }

        @Override
        public Collection<NodeProvisioner.PlannedNode> provision(CloudState state, int excessWorkload) {
            provisionCalls++;
            return planned;
        }
    }

    @TestExtension
    public static class RecordingProvisioningListener extends CloudProvisioningListener {
        static final List<NodeProvisioner.PlannedNode> started = new CopyOnWriteArrayList<>();

        @Override
        public void onStarted(Cloud cloud, Label label, Collection<NodeProvisioner.PlannedNode> plannedNodes) {
            started.addAll(plannedNodes);
        }
    }
}
