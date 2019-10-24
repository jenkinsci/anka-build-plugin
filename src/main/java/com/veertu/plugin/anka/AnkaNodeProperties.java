package com.veertu.plugin.anka;

import hudson.model.Node;
import hudson.slaves.RetentionStrategy;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.ArrayList;
import java.util.List;

public class AnkaNodeProperties {

    @DataBoundConstructor
    public AnkaNodeProperties() {
    }

    public String getMasterVmId() {
        return valOrNull(masterVmId);
    }

    @DataBoundSetter
    public void setMasterVmId(String masterVmId) {
        this.masterVmId = masterVmId;
    }

    public String getTag() {
        return valOrNull(tag);
    }

    @DataBoundSetter
    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getLaunchDelay() {
        return launchDelay;
    }

    @DataBoundSetter
    public void setLaunchDelay(int launchDelay) {
        this.launchDelay = launchDelay;
    }

    public String getRemoteFS() {
        return valOrNull(remoteFS);
    }

    @DataBoundSetter
    public void setRemoteFS(String remoteFS) {
        this.remoteFS = remoteFS;
    }

    public String getLabel() {
        return valOrNull(label);
    }

    @DataBoundSetter
    public void setLabel(String labelString) {
        this.label = labelString;
    }

    public String getTemplateDescription() {
        return valOrNull(templateDescription);
    }

    @DataBoundSetter
    public void setTemplateDescription(String templateDescription) {
        this.templateDescription = templateDescription;
    }

    public int getNumberOfExecutors() {
        return numberOfExecutors;
    }

    @DataBoundSetter
    public void setNumberOfExecutors(int numberOfExecutors) {
        this.numberOfExecutors = numberOfExecutors;
    }

    public Node.Mode getMode() {
        return mode;
    }

    @DataBoundSetter
    public void setMode(Node.Mode mode) {
        this.mode = mode;
    }

    public String getCredentialsId() {
        return valOrNull(credentialsId);
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getGroup() {
        return valOrNull(group);
    }

    @DataBoundSetter
    public void setGroup(String group) {
        this.group = group;
    }

    public String getExtraArgs() {
        return valOrNull(extraArgs);
    }

    @DataBoundSetter
    public void setExtraArgs(String extraArgs) {
        this.extraArgs = extraArgs;
    }

    public String getLaunchMethod() {
        return valOrNull(launchMethod);
    }

    @DataBoundSetter
    public void setLaunchMethod(String launchMethod) {
        this.launchMethod = launchMethod;
    }


    public boolean isKeepAliveOnError() {
        return keepAliveOnError;
    }

    @DataBoundSetter
    public void setKeepAliveOnError(boolean keepAliveOnError) {
        this.keepAliveOnError = keepAliveOnError;
    }

    public int getSSHPort() {
        return SSHPort;
    }

    @DataBoundSetter
    public void setSSHPort(int SSHPort) {
        this.SSHPort = SSHPort;
    }

    public List<AnkaCloudSlaveTemplate.EnvironmentEntry> getEnvironments() {
        if (environments != null) {
            return environments;
        }
        return new ArrayList<AnkaCloudSlaveTemplate.EnvironmentEntry>();
    }

    @DataBoundSetter
    public void setEnvironments(List<AnkaCloudSlaveTemplate.EnvironmentEntry> environments) {
        this.environments = environments;
    }

    public RetentionStrategy getRetentionStrategy() {
        return retentionStrategy;
    }

    @DataBoundSetter
    public void setRetentionStrategy(RetentionStrategy retentionStrategy) {
        this.retentionStrategy = retentionStrategy;
    }

    public String getNameTemplate() {
        return valOrNull(nameTemplate);
    }

    @DataBoundSetter
    public void setNameTemplate(String nameTemplate) {
        this.nameTemplate = nameTemplate;
    }

    public String getJavaArgs() {
        return valOrNull(javaArgs);
    }

    @DataBoundSetter
    public void setJavaArgs(String javaArgs) {
        this.javaArgs = javaArgs;
    }

    public String getJnlpJenkinsOverrideUrl() {
        return valOrNull(jnlpJenkinsOverrideUrl);
    }

    @DataBoundSetter
    public void setJnlpJenkinsOverrideUrl(String jnlpJenkinsOverrideUrl) {
        this.jnlpJenkinsOverrideUrl = jnlpJenkinsOverrideUrl;
    }

    public String getJnlpTunnel() {
        return valOrNull(jnlpTunnel);
    }

    @DataBoundSetter
    public void setJnlpTunnel(String jnlpTunnel) {
        this.jnlpTunnel = jnlpTunnel;
    }

    public int getPriority() {
        return priority;
    }

    @DataBoundSetter
    public void setPriority(int priority) {
        this.priority = priority;
    }

    protected String valOrNull(String val) {
        if (val == null || val.isEmpty()) {
            return null;
        }
        return val;
    }

    protected String masterVmId;
    protected String tag;
    protected int launchDelay;
    protected String remoteFS;
    protected String label;
    protected String templateDescription;
    protected int numberOfExecutors;
    protected Node.Mode mode;
    protected String credentialsId;
    protected String group;
    protected String extraArgs;
    protected String launchMethod;
    protected boolean keepAliveOnError;
    protected int SSHPort;
    protected List<AnkaCloudSlaveTemplate.EnvironmentEntry> environments;
    protected RetentionStrategy retentionStrategy = new RunOnceCloudRetentionStrategy(1);
    protected String nameTemplate;
    protected String javaArgs;
    protected String jnlpJenkinsOverrideUrl;
    protected String jnlpTunnel;
    protected int priority;
}
