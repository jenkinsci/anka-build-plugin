package com.veertu.plugin.anka;

import hudson.model.Descriptor;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.NodeProperty;
import jenkins.model.Jenkins;
import org.apache.commons.lang.RandomStringUtils;

import java.io.IOException;
import java.util.List;

/**
 * Created by asafgur on 16/11/2016.
 */
public class AnkaOnDemandSlave extends AbstractAnkaSlave {

    protected AnkaOnDemandSlave(AnkaMgmtCloud cloud, String name, String nodeDescription, String remoteFS, int numExecutors,
                                Mode mode, String labelString, ComputerLauncher launcher,
                                List<? extends NodeProperty<?>> nodeProperties,
                                AnkaCloudSlaveTemplate template, String vmId) throws Descriptor.FormException, IOException {
        super(cloud, name, nodeDescription, remoteFS, numExecutors, mode, labelString,
                launcher, template.getRetentionStrategy(), nodeProperties, template, vmId);
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
        String randomString = RandomStringUtils.randomAlphanumeric(5);
        String nameTemplate = template.getNameTemplate();
        if (nameTemplate != null && !nameTemplate.isEmpty()) {
            nameTemplate = nameTemplate.replace("$instance_id", "");
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
        if (template.getCloudName() != null && !template.getCloudName().isEmpty()) {
            nodeNameBuilder.append(template.getCloudName());
            nodeNameBuilder.append("-");
        }
        nodeNameBuilder.append(template.getLabelString());
        nodeNameBuilder.append("-");
        nodeNameBuilder.append(randomString);
        return nodeNameBuilder.toString().replaceAll(" ", "");
    }


    public static String createStartUpScript(AnkaCloudSlaveTemplate template, String nodeName) {
        // String startUpScript = ""; // implement in the future
        if (template.getLaunchMethod().equalsIgnoreCase(LaunchMethod.JNLP)) {
            return JnlpCommandBuilder.makeStartUpScript(nodeName, template.getExtraArgs(), template.getJavaArgs(), template.getJnlpJenkinsOverrideUrl());
        }
        return null;
    }

    @Override
    public SlaveDescriptor getDescriptor() {
        return super.getDescriptor();
    }
}
