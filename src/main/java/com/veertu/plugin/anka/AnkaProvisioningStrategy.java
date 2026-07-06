/*
 * The MIT License
 *
 * Copyright 2014 CloudBees.
 * Copyright (c) 2015 Kanstantsin Shautsou
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */


package com.veertu.plugin.anka;

import hudson.Extension;
import hudson.model.Label;
import hudson.model.LoadStatistics;
import hudson.model.Queue;
import hudson.model.queue.QueueListener;
import hudson.slaves.Cloud;
import hudson.slaves.CloudProvisioningListener;
import hudson.slaves.NodeProvisioner;
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * Fast provisioning strategy for one-shot Anka VM agents.
 *
 * <p>The strategy itself is intentionally trivial: it compares queued demand against the executors
 * that already exist or are on their way (available + connecting + Jenkins' native planned-capacity
 * snapshot) and asks the cloud to provision the difference. It does not try to second-guess the
 * LoadStatistics snapshot. The defense against over-provisioning during the boot/connect window lives
 * in {@link AnkaMgmtCloud#provision}, which tracks nodes it has started but not yet handed to Jenkins
 * and refuses to start more than are actually needed.
 */
@Extension
public class AnkaProvisioningStrategy extends NodeProvisioner.Strategy {

    // this function gets called when jenkins needs more of a specific label
    @Nonnull
    @Override
    public NodeProvisioner.StrategyDecision apply(@Nonnull NodeProvisioner.StrategyState strategyState) {
        if (Jenkins.get().isQuietingDown()) {
            return NodeProvisioner.StrategyDecision.CONSULT_REMAINING_STRATEGIES;
        }
        for (Cloud cloud : Jenkins.get().clouds) {
            if (cloud instanceof AnkaMgmtCloud ankaCloud) {
                NodeProvisioner.StrategyDecision decision = applyToCloud(strategyState, ankaCloud);
                if (decision == NodeProvisioner.StrategyDecision.PROVISIONING_COMPLETED) {
                    return decision;
                }
            }
        }
        return NodeProvisioner.StrategyDecision.CONSULT_REMAINING_STRATEGIES;
    }

    private NodeProvisioner.StrategyDecision applyToCloud(@Nonnull NodeProvisioner.StrategyState strategyState,
                                                          AnkaMgmtCloud cloud) {
        final Label label = strategyState.getLabel();
        final Cloud.CloudState cloudState = new Cloud.CloudState(label, strategyState.getAdditionalPlannedCapacity());
        if (!cloud.canProvision(cloudState)) {
            return NodeProvisioner.StrategyDecision.CONSULT_REMAINING_STRATEGIES;
        }

        LoadStatistics.LoadStatisticsSnapshot snap = strategyState.getSnapshot();
        int availableCapacity = snap.getAvailableExecutors()
                + snap.getConnectingExecutors()
                + strategyState.getPlannedCapacitySnapshot();
        int currentDemand = snap.getQueueLength();
        AnkaMgmtCloud.Log("Available capacity=%d, currentDemand=%d", availableCapacity, currentDemand);

        if (availableCapacity < currentDemand) {
            Collection<NodeProvisioner.PlannedNode> plannedNodes = cloud.provision(cloudState, currentDemand - availableCapacity);
            AnkaMgmtCloud.Log("Planned %d new nodes", plannedNodes.size());
            fireOnStarted(cloud, label, plannedNodes);
            strategyState.recordPendingLaunches(plannedNodes);
            availableCapacity += plannedNodes.size();
            AnkaMgmtCloud.Log("After provisioning, available capacity=%d, currentDemand=%d", availableCapacity, currentDemand);
        }

        if (availableCapacity >= currentDemand) {
            AnkaMgmtCloud.Log("Provisioning completed");
            return NodeProvisioner.StrategyDecision.PROVISIONING_COMPLETED;
        }
        AnkaMgmtCloud.Log("Provisioning not complete, consulting remaining strategies");
        return NodeProvisioner.StrategyDecision.CONSULT_REMAINING_STRATEGIES;
    }

    /**
     * Fire {@code onStarted} on all {@link CloudProvisioningListener}s for the given planned nodes.
     * Mirrors {@code hudson.slaves.NodeProvisioner#fireOnStarted}: because this custom strategy calls
     * {@link Cloud#provision} directly, it must fire the listener itself, otherwise plugins such as
     * cloud-stats never see a {@code ProvisioningActivity} for these nodes and throw
     * "No activity tracked for ...".
     */
    private static void fireOnStarted(Cloud cloud, Label label, Collection<NodeProvisioner.PlannedNode> plannedNodes) {
        for (CloudProvisioningListener cl : CloudProvisioningListener.all()) {
            try {
                cl.onStarted(cloud, label, plannedNodes);
            } catch (Error e) {
                throw e;
            } catch (Throwable e) {
                AnkaMgmtCloud.Log("Unexpected uncaught exception in onStarted() of %s for label %s: %s",
                        cl, label, e);
            }
        }
    }

    /**
     * Ping the nodeProvisioner as a new task enters the queue, so it can provision an Anka agent
     * without waiting for the next NodeProvisioner cycle.
     */
    @Extension
    public static class FastProvisioning extends QueueListener {
        @Override
        public void onEnterBuildable(Queue.BuildableItem item) {
            suggestReviewForBuildableLabel(Jenkins.get(), item.getAssignedLabel());
        }
    }

    static void suggestReviewForBuildableLabel(Jenkins jenkins, Label label) {
        final Cloud.CloudState cloudState = new Cloud.CloudState(label, 0);
        for (Cloud cloud : jenkins.clouds) {
            if (cloud instanceof AnkaMgmtCloud && cloud.canProvision(cloudState)) {
                final NodeProvisioner provisioner =
                        label == null ? jenkins.unlabeledNodeProvisioner : label.nodeProvisioner;
                provisioner.suggestReviewNow();
            }
        }
    }
}
