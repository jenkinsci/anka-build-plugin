package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.ComputerListener;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.NodeProperty;
import jenkins.slaves.RemotingWorkDirSettings;
import org.apache.commons.lang.RandomStringUtils;

import java.io.IOException;
import java.util.List;

/**
 * Created by asafgur on 16/11/2016.
 */
public class AnkaOnDemandSlave extends AbstractAnkaSlave {

    private boolean acceptingTasks = true;

    protected AnkaOnDemandSlave(String name, String nodeDescription, String remoteFS, int numExecutors,
                                Mode mode, String labelString, ComputerLauncher launcher,
                                List<? extends NodeProperty<?>> nodeProperties,
                                AnkaCloudSlaveTemplate template, AnkaMgmtVm vm) throws Descriptor.FormException, IOException {
        super(name, nodeDescription, remoteFS, numExecutors, mode, labelString,
                launcher, template.getRetentionStrategy(), nodeProperties, template, vm);
    }


    public static String generateName(AnkaCloudSlaveTemplate template) {
        String randomString = RandomStringUtils.randomAlphanumeric(16);
        String nameTemplate = template.getNameTemplate();
        if (nameTemplate != null && !nameTemplate.isEmpty()) {
            nameTemplate = nameTemplate.replace("$intance_id", "");
            nameTemplate = nameTemplate.replace("$node_id", "");
            nameTemplate = nameTemplate.replace("$node_name", "");
            nameTemplate = nameTemplate.replace("$template_name", "");
            nameTemplate = nameTemplate.replace("$cloud_name", template.getCloudName());
            nameTemplate = nameTemplate.replace("$label", template.getLabelString());
            nameTemplate = nameTemplate.replace("$template_id", template.getMasterVmId());
            if (template.getTag() != null && !template.getTag().isEmpty()) {
                nameTemplate = nameTemplate.replace("$tag", template.getTag());
            }
            if (nameTemplate.contains("$ts")) {
                Long unixTime = System.currentTimeMillis() / 1000L;
                nameTemplate = nameTemplate.replace("$ts", unixTime.toString());
                return nameTemplate;
            }
            return nameTemplate + "_" + randomString;
        }
        StringBuilder nodeNameBuilder = new StringBuilder();
        nodeNameBuilder.append(template.getCloudName());
        nodeNameBuilder.append("-");
        nodeNameBuilder.append(template.getLabelString());
        nodeNameBuilder.append("-");
        nodeNameBuilder.append(template.getMasterVmId());
        nodeNameBuilder.append("-");
        if (template.getTag() != null && !template.getTag().isEmpty()) {
            nodeNameBuilder.append(template.getTag());
            nodeNameBuilder.append("-");
        }
        nodeNameBuilder.append(randomString);
        return nodeNameBuilder.toString().replaceAll(" ", "");
    }


    public static AnkaOnDemandSlave createProvisionedSlave(AnkaMgmtCloud cloud, AnkaCloudSlaveTemplate template)
            throws IOException, AnkaMgmtException, Descriptor.FormException, InterruptedException {
        if (template.getLaunchMethod().toLowerCase().equals(LaunchMethod.SSH)) {
            return createSSHSlave(cloud, template);
        } else if (template.getLaunchMethod().toLowerCase().equals(LaunchMethod.JNLP)) {
            return createJNLPSlave(cloud, template);
        }
        return null;
    }

    private static AnkaOnDemandSlave createJNLPSlave(AnkaMgmtCloud cloud, final AnkaCloudSlaveTemplate template) throws AnkaMgmtException, IOException, Descriptor.FormException {
//        AnkaMgmtCloud.Log("vm %s is booting...", vm.getId());
        String nodeName = generateName(template);
        String jnlpCommand = JnlpCommandBuilder.makeStartUpScript(nodeName, template.getExtraArgs(), template.getJavaArgs(), template.getJnlpJenkinsOverrideUrl());

        final AnkaMgmtVm vm = cloud.startVMInstance(
                template.getMasterVmId(), template.getTag(), template.getNameTemplate(), template.getSSHPort(), jnlpCommand, template.getGroup(), template.getPriority());
        AnkaMgmtCloud.Log("vm %s %s is booted, creating jnlp launcher", vm.getId(), vm.getName());

        String tunnel = "";
        JNLPLauncher launcher = new JNLPLauncher(template.getJnlpTunnel(),
                "",
                RemotingWorkDirSettings.getEnabledDefaults());
        AnkaMgmtCloud.Log("launcher created for vm %s %s", vm.getId(), vm.getName());
        AnkaOnDemandSlave slave = new AnkaOnDemandSlave(nodeName, template.getTemplateDescription(), template.getRemoteFS(),
                template.getNumberOfExecutors(),
                template.getMode(),
                template.getLabelString(),
                launcher,
                template.getNodeProperties(), template, vm);
        slave.register();

        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    vm.waitForBoot(template.getSchedulingTimeout());
                } catch (InterruptedException | IOException | AnkaMgmtException e) {
                    vm.terminate();
                    throw new RuntimeException(new AnkaMgmtException(e));
                }



            }
        }).run();


        slave.setDisplayName(vm.getName());
        return slave;
    }

    private static AnkaOnDemandSlave createSSHSlave(AnkaMgmtCloud cloud, final AnkaCloudSlaveTemplate template) throws InterruptedException, AnkaMgmtException, IOException, Descriptor.FormException {
        final AnkaMgmtVm vm = cloud.startVMInstance(
                template.getMasterVmId(), template.getTag(), template.getNameTemplate(), template.getSSHPort(), null, template.getGroup(), template.getPriority());
        try {
            AnkaOnDemandSlave slave = new AnkaOnDemandSlave(generateName(template), template.getTemplateDescription(), template.getRemoteFS(),
                    template.getNumberOfExecutors(),
                    template.getMode(),
                    template.getLabelString(),
                    null,
                    template.getNodeProperties(), template, vm);
            AnkaMgmtCloud.Log("vm %s is booting...", vm.getId());
            try {
                vm.waitForBoot(template.getSchedulingTimeout());
            } catch (InterruptedException | IOException | AnkaMgmtException e) {
                vm.terminate();
                throw new RuntimeException(new AnkaMgmtException(e));
            }
            AnkaMgmtCloud.Log("vm %s %s is booted, creating ssh launcher", vm.getId(), vm.getName());
            SSHLauncher launcher = new SSHLauncher(vm.getConnectionIp(), vm.getConnectionPort(),
                    template.getCredentialsId(),
                    template.getJavaArgs(), null, null, null, launchTimeoutSeconds, maxNumRetries, retryWaitTime, null);

            slave.setLauncher(launcher);
            String name = vm.getName();
            if (name != null){
                slave.setNodeName(name);
            }
            slave.register();
            AnkaMgmtCloud.Log("launcher created for vm %s %s", vm.getId(), vm.getName());
            return slave;
        } catch (Exception e) {
            e.printStackTrace();
            vm.terminate();
            throw e;
        }
    }


    public void setDescription(String jobAndNumber) {
        String description = String.format("master image: %s, job name and build number: %s, vm info: (%s)",
                template.getMasterVmId(), jobAndNumber, this.vm.getInfo());
        super.setNodeDescription(description);

    }


    public boolean isKeepAliveOnError() {
        return this.template.isKeepAliveOnError();
    }

    public boolean canTerminate() {
        if (hadProblemsInBuild) {
            if (isKeepAliveOnError()) {
                return false;
            }
        }
        return true;
    }

    public void setHadErrorsOnBuild(boolean value) {
        this.hadProblemsInBuild = value;
    }

    @Extension
    public static class VeertuCloudComputerListener extends ComputerListener {

        @Override
        public void preLaunch(Computer c, TaskListener taskListener) throws IOException, InterruptedException {
            super.preLaunch(c, taskListener);
        }
    }

    @Override
    public SlaveDescriptor getDescriptor() {
        return super.getDescriptor();
    }
}
