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
        // Label objLabel;
        // try {
        //     objLabel = Label.parseExpression(label);
        // } catch (ANTLRException e) {
        //     e.printStackTrace();
        //     throw new AnkaMgmtException(e);
        // }
        if (template.getLaunchMethod().toLowerCase().equals(LaunchMethod.SSH)) {
            return AnkaOnDemandSlave.createSSHSlave(cloud, template);
        } else if (template.getLaunchMethod().toLowerCase().equals(LaunchMethod.JNLP)) {
            return AnkaOnDemandSlave.createJNLPSlave(cloud, template);
        }
        return null;
    }

//
//    protected static AnkaOnDemandSlave createJNLPSlave(AnkaMgmtCloud cloud, AnkaCloudSlaveTemplate template) throws AnkaMgmtException, IOException, Descriptor.FormException {
////        AnkaMgmtCloud.Log("vm %s is booting...", vm.getId());
//        String nodeName = generateName(template);
//        String jnlpCommand = JnlpCommandBuilder.makeStartUpScript(nodeName, template.getExtraArgs(), template.getJavaArgs(), template.getJnlpJenkinsOverrideUrl());
//
//        AnkaMgmtVm vm = cloud.startVMInstance(
//                template.getMasterVmId(), template.getTag(), template.getNameTemplate(),
//                template.getSSHPort(), jnlpCommand, template.getGroup(), template.getPriority());
//        try {
//            vm.waitForBoot(template.getSchedulingTimeout());
//        } catch (InterruptedException| IOException|AnkaMgmtException e) {
//            vm.terminate();
//            throw new AnkaMgmtException(e);
//        }
//        AnkaMgmtCloud.Log("vm %s %s is booted, creating jnlp launcher", vm.getId(), vm.getName());
//
//        String tunnel = "";
//        JNLPLauncher launcher = new JNLPLauncher(template.getJnlpTunnel(),
//                "",
//                RemotingWorkDirSettings.getEnabledDefaults());
//        ArrayList<EnvironmentVariablesNodeProperty.Entry> a = new ArrayList<EnvironmentVariablesNodeProperty.Entry>();
//        for (AnkaCloudSlaveTemplate.EnvironmentEntry e :template.getEnvironments()) {
//            a.add(new EnvironmentVariablesNodeProperty.Entry(e.name, e.value));
//        }
//
//        EnvironmentVariablesNodeProperty env = new EnvironmentVariablesNodeProperty(a);
//        ArrayList<NodeProperty<?>> props = new ArrayList<>();
//        props.add(env);
//
//        AnkaMgmtCloud.Log("launcher created for vm %s %s", vm.getId(), vm.getName());
//        AnkaOnDemandSlave slave = new AnkaOnDemandSlave(nodeName, template.getTemplateDescription(), template.getRemoteFS(),
//                template.getNumberOfExecutors(),
//                template.getMode(),
//                template.getLabelString(),
//                launcher,
//                props, template, vm);
//        slave.setDisplayName(vm.getName());
//        return slave;
//    }
//
//    protected static AnkaOnDemandSlave createSSHSlave(AnkaMgmtCloud cloud, AnkaCloudSlaveTemplate template) throws InterruptedException, AnkaMgmtException, IOException, Descriptor.FormException {
//        AnkaMgmtVm vm = cloud.startVMInstance(
//                template.getMasterVmId(), template.getTag(), template.getNameTemplate(), template.getSSHPort(), null, template.getGroup(), template.getPriority());
//        try {
//
//            ArrayList<EnvironmentVariablesNodeProperty.Entry> a = new ArrayList<EnvironmentVariablesNodeProperty.Entry>();
//            for (AnkaCloudSlaveTemplate.EnvironmentEntry e : template.getEnvironments()) {
//                a.add(new EnvironmentVariablesNodeProperty.Entry(e.name, e.value));
//            }
//
//            EnvironmentVariablesNodeProperty env = new EnvironmentVariablesNodeProperty(a);
//            ArrayList<NodeProperty<?>> props = new ArrayList<>();
//            props.add(env);
//
//            AnkaOnDemandSlave slave = new AnkaOnDemandSlave(vm.getId(), template.getTemplateDescription(), template.getRemoteFS(),
//                    template.getNumberOfExecutors(),
//                    template.getMode(),
//                    template.getLabelString(),
//                    null,
//                    props, template, vm);
//            AnkaMgmtCloud.Log("vm %s is booting...", vm.getId());
//            try {
//                vm.waitForBoot(template.getSchedulingTimeout());
//            } catch (InterruptedException| IOException|AnkaMgmtException e) {
//                vm.terminate();
//                throw new AnkaMgmtException(e);
//            }
//            AnkaMgmtCloud.Log("vm %s %s is booted, creating ssh launcher", vm.getId(), vm.getName());
//            SSHLauncher launcher = new SSHLauncher(vm.getConnectionIp(), vm.getConnectionPort(),
//                    template.getCredentialsId(),
//                    template.getJavaArgs(), null, null, null, launchTimeoutSeconds, maxNumRetries, retryWaitTime, null);
//
//
//
//            slave.setLauncher(launcher);
//
//            AnkaMgmtCloud.Log("launcher created for vm %s %s", vm.getId(), vm.getName());
//            String name = vm.getName();
//            slave.setNodeName(name);
//            return slave;
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//            vm.terminate();
//            throw e;
//        }
//    }

}




