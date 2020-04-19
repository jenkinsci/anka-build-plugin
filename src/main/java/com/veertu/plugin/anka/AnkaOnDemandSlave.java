package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import hudson.model.Descriptor;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.NodeProperty;
import jenkins.model.Jenkins;
import org.apache.commons.lang.RandomStringUtils;

import java.io.IOException;
import java.util.List;

/**
 * Created by asafgur on 16/11/2016.
 */
public class AnkaOnDemandSlave extends AbstractAnkaSlave {

    protected AnkaOnDemandSlave(String name, String nodeDescription, String remoteFS, int numExecutors,
                                Mode mode, String labelString, ComputerLauncher launcher,
                                List<? extends NodeProperty<?>> nodeProperties,
                                AnkaCloudSlaveTemplate template, AnkaMgmtVm vm) throws Descriptor.FormException, IOException {
        super(name, nodeDescription, remoteFS, numExecutors, mode, labelString,
                launcher, template.getRetentionStrategy(), nodeProperties, template, vm);
    }

    public static String getJenkinsNodeLink(String nodeName) {
        String effectiveJenkinsUrl = Jenkins.get().getRootUrl();
        if (effectiveJenkinsUrl == null) {
            return String.format("/computer/%s", nodeName);
        }
        String nodeFormat = "%s/computer/%s";
        if (effectiveJenkinsUrl.endsWith("/")) {
            nodeFormat = "%scomputer/%s";
        }
        return String.format(nodeFormat, effectiveJenkinsUrl, nodeName);

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

    protected static AnkaOnDemandSlave createJNLPSlave(AnkaMgmtCloud cloud, final AnkaCloudSlaveTemplate template) throws AnkaMgmtException, IOException, Descriptor.FormException {
//        AnkaMgmtCloud.Log("vm %s is booting...", vm.getId());
        String nodeName = generateName(template);
        String jnlpCommand = JnlpCommandBuilder.makeStartUpScript(nodeName, template.getExtraArgs(), template.getJavaArgs(), template.getJnlpJenkinsOverrideUrl());

        final AnkaMgmtVm vm = cloud.startVMInstance(
                template.getMasterVmId(), template.getTag(), template.getNameTemplate(),
                template.getSSHPort(), jnlpCommand, template.getGroup(), template.getPriority(), nodeName, getJenkinsNodeLink(nodeName));
        AnkaMgmtCloud.Log("vm %s %s is booted, creating jnlp launcher", vm.getId(), vm.getName());

        JNLPLauncher launcher = new JNLPLauncher(template.getJnlpTunnel(),
                template.getExtraArgs());
        AnkaMgmtCloud.Log("launcher created for vm %s %s", vm.getId(), vm.getName());
        AnkaOnDemandSlave slave = new AnkaOnDemandSlave(nodeName, template.getTemplateDescription(), template.getRemoteFS(),
                template.getNumberOfExecutors(),
                template.getMode(),
                template.getLabelString(),
                launcher,
                template.getNodeProperties(), template, vm);
        slave.register();


        try {
            vm.waitForBoot(template.getSchedulingTimeout());
        } catch (InterruptedException | IOException | AnkaMgmtException e) {
            vm.terminate();
            throw new AnkaMgmtException(e);
        }

        slave.setDisplayName(vm.getName());
        return slave;
    }

    protected static AnkaOnDemandSlave createSSHSlave(AnkaMgmtCloud cloud, final AnkaCloudSlaveTemplate template) throws InterruptedException, AnkaMgmtException, IOException, Descriptor.FormException {
        String nodeName = generateName(template);

        final AnkaMgmtVm vm = cloud.startVMInstance(
                template.getMasterVmId(), template.getTag(), template.getNameTemplate(), template.getSSHPort(), null, template.getGroup(), template.getPriority(), null, null );
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
            if (name != null ) {
                slave.setNodeName(name);
                try {
                    cloud.updateInstance(vm, name, getJenkinsNodeLink(name), null);
                } catch (AnkaMgmtException e) {
                    AnkaMgmtCloud.Log("Name update failed: ", e.getMessage());
                    e.printStackTrace();
                }
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

    @Override
    public SlaveDescriptor getDescriptor() {
        return super.getDescriptor();
    }
}
