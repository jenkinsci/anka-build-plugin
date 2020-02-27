///*
// * The MIT License
// *
// * Copyright 2014 CloudBees.
// * Copyright (c) 2015 Kanstantsin Shautsou
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy
// * of this software and associated documentation files (the "Software"), to deal
// * in the Software without restriction, including without limitation the rights
// * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// * copies of the Software, and to permit persons to whom the Software is
// * furnished to do so, subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in
// * all copies or substantial portions of the Software.
// *
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// * THE SOFTWARE.
// */
//
//
//package com.veertu.plugin.anka;
//
//import hudson.model.LoadStatistics.LoadStatisticsSnapshot;
//import hudson.slaves.NodeProvisioner;
//import hudson.slaves.NodeProvisioner.PlannedNode;
//import hudson.Extension;
//import jenkins.model.Jenkins;
//import hudson.slaves.Cloud;
//import hudson.model.Label;
//
//import java.util.List;
//import java.util.Collection;
//import java.util.ArrayList;
//import javax.annotation.Nonnull;
//
//@Extension
//public class AnkaProvisioningStrategy extends NodeProvisioner.Strategy {
//
//    private static List<AnkaMgmtCloud> getAnkaClouds() {
//
//        List<AnkaMgmtCloud> clouds = new ArrayList<AnkaMgmtCloud>();
//        final Jenkins jenkins = Jenkins.getInstance();
//        for (Cloud cloud : jenkins.clouds) {
//            if (cloud instanceof AnkaMgmtCloud) {
//                AnkaMgmtCloud ankaCloud = (AnkaMgmtCloud) cloud;
//                clouds.add(ankaCloud);
//            }
//        }
//        return clouds;
//    }
//
//
//    @Nonnull
//    @Override
//    public NodeProvisioner.StrategyDecision apply(@Nonnull NodeProvisioner.StrategyState strategyState) {
//        AnkaMgmtCloud.Log("Applying provisioning");
//        final Label label = strategyState.getLabel();
//        LoadStatisticsSnapshot snapshot = strategyState.getSnapshot();
//
//        for (AnkaMgmtCloud ankaCloud : getAnkaClouds()) {
//            AnkaCloudSlaveTemplate template = ankaCloud.getTemplate(label);
//            if (template == null)
//                continue;
//
//            int availableCapacity = snapshot.getAvailableExecutors() +
//                        snapshot.getConnectingExecutors() +
//                        strategyState.getAdditionalPlannedCapacity() +
//                        strategyState.getPlannedCapacitySnapshot();
//
//            int currentDemand = snapshot.getQueueLength();
//
//            AnkaMgmtCloud.Log("Available capacity=%d, currentDemand=%d", availableCapacity, currentDemand);
//
//            if (availableCapacity < currentDemand) {
//                // may happen that would be provisioned with other template
//                Collection<PlannedNode> plannedNodes = ankaCloud.provision(label, currentDemand - availableCapacity);
//
//                strategyState.recordPendingLaunches(plannedNodes);
//                // FIXME calculate executors number?
//                availableCapacity += plannedNodes.size();
//                AnkaMgmtCloud.Log("After AnkaCloud provisioning, available capacity=%d, currentDemand=%d",
//                        availableCapacity, currentDemand);
//            }
//
//            if (availableCapacity >= currentDemand) {
//                return NodeProvisioner.StrategyDecision.PROVISIONING_COMPLETED;
//            }
//
//
//            AnkaMgmtCloud.Log("Provisioning not complete, trying next Anka Cloud");
//        }
//
//        AnkaMgmtCloud.Log("Provisioning not complete, consulting remaining strategies");
//        return NodeProvisioner.StrategyDecision.CONSULT_REMAINING_STRATEGIES;
//    }
//}