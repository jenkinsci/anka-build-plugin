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
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;
import java.util.Collection;

@Extension
public class AnkaProvisioningStrategy extends NodeProvisioner.Strategy {

    // this function gets called when jenkins need more of a specific label
    @Nonnull
    @Override
    public NodeProvisioner.StrategyDecision apply(@Nonnull NodeProvisioner.StrategyState strategyState) {
        final Label label = strategyState.getLabel();
        int availableCapacity;
        int currentDemand;
        synchronized (this) {
            LoadStatistics.LoadStatisticsSnapshot snap = strategyState.getSnapshot();
            availableCapacity = snap.getAvailableExecutors()
                    + snap.getConnectingExecutors()
                    + strategyState.getPlannedCapacitySnapshot()
                    + strategyState.getAdditionalPlannedCapacity();
            currentDemand = snap.getQueueLength();

            AnkaMgmtCloud.Log("Available capacity=%s, currentDemand=%s", availableCapacity, currentDemand);
            if (currentDemand > availableCapacity) {
                Jenkins jenkinsInstance = Jenkins.get();
                Cloud.CloudState cloudState = new Cloud.CloudState(label, strategyState.getAdditionalPlannedCapacity());
                for (Cloud cloud : jenkinsInstance.clouds) {
                    if (cloud instanceof AnkaMgmtCloud && cloud.canProvision(cloudState)) {
                        Collection<NodeProvisioner.PlannedNode> plannedNodes = cloud.provision(cloudState, currentDemand - availableCapacity);
                        AnkaMgmtCloud.Log(String.format("Planned %d new nodes", plannedNodes.size()));
                        strategyState.recordPendingLaunches(plannedNodes);
                        availableCapacity += plannedNodes.size();
                        AnkaMgmtCloud.Log("After provisioning, available capacity=%d, currentDemand=%d", availableCapacity, currentDemand);
                        break;
                    }

                }
            }
        }
        if (availableCapacity >= currentDemand) {
            AnkaMgmtCloud.Log("Provisioning completed");
            return NodeProvisioner.StrategyDecision.PROVISIONING_COMPLETED;
        } else {
            AnkaMgmtCloud.Log("Provisioning not complete, consulting remaining strategies");
            return NodeProvisioner.StrategyDecision.CONSULT_REMAINING_STRATEGIES;
        }
    }
}