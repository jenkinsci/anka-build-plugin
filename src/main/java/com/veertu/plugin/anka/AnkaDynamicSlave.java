package com.veertu.plugin.anka;

import hudson.model.Descriptor;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.NodeProperty;

import java.io.IOException;
import java.util.List;

public class AnkaDynamicSlave extends AnkaOnDemandSlave {

    protected AnkaDynamicSlave(AnkaMgmtCloud cloud, String name, String nodeDescription, String remoteFS, int numExecutors, Mode mode, String labelString, ComputerLauncher launcher, List<? extends NodeProperty<?>> nodeProperties, AnkaCloudSlaveTemplate template, String vmId) throws Descriptor.FormException, IOException {
        super(cloud, name, nodeDescription, remoteFS, numExecutors, mode, labelString, launcher, nodeProperties, template, vmId);
    }
}
