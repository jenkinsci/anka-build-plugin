package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import hudson.model.Descriptor;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.NodeProperty;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DynamicSlave extends AnkaOnDemandSlave {

    public DynamicSlave(AnkaOnDemandSlave slave) throws IOException, Descriptor.FormException {
        this(slave.getDisplayName(), slave.getNodeDescription(), slave.getRemoteFS(), slave.getNumExecutors(),
                slave.getMode(), slave.getLabelString(), slave.getComputer().getLauncher(),
                slave.getNodeProperties(), slave.getTemplate(), slave.getVM());

    }

    protected DynamicSlave(String name, String nodeDescription, String remoteFS, int numExecutors,
                           Mode mode, String labelString, ComputerLauncher launcher,
                           List<? extends NodeProperty<?>> nodeProperties,
                           AnkaCloudSlaveTemplate template, AnkaMgmtVm vm) throws Descriptor.FormException, IOException {
        super(name, nodeDescription, remoteFS, numExecutors, mode, labelString, launcher, nodeProperties, template, vm);
    }


    public static AnkaOnDemandSlave createDynamicSlave(AnkaMgmtCloud cloud, DynamicSlaveProperties properties, String label) throws InterruptedException, Descriptor.FormException, AnkaMgmtException, IOException, ExecutionException {
        AnkaCloudSlaveTemplate template = properties.toSlaveTemplate(label);
        template.setCloudName(cloud.getCloudName());

        if (template.getLaunchMethod().toLowerCase().equals(LaunchMethod.SSH)) {
            return AnkaOnDemandSlave.createSSHSlave(cloud, template);
        } else if (template.getLaunchMethod().toLowerCase().equals(LaunchMethod.JNLP)) {
            return AnkaOnDemandSlave.createJNLPSlave(cloud, template);
        }
        return null;
    }



}




