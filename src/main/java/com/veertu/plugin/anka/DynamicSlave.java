package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import hudson.model.Descriptor;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.NodeProperty;

import java.io.IOException;
import java.util.List;

public class DynamicSlave extends AnkaOnDemandSlave {

    protected DynamicSlave(String name, String nodeDescription, String remoteFS, int numExecutors,
                           Mode mode, String labelString, ComputerLauncher launcher,
                           List<? extends NodeProperty<?>> nodeProperties,
                           AnkaCloudSlaveTemplate template, AnkaMgmtVm vm) throws Descriptor.FormException, IOException {
        super(name, nodeDescription, remoteFS, numExecutors, mode, labelString, launcher, nodeProperties, template, vm);
    }

//    public static DynamicSlave createDynamicSlave( ) {
//
//    }
}
