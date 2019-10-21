package com.veertu.plugin.anka;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.slaves.RetentionStrategy;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.List;

public class DynamicSlaveProperties extends Step {

    public String getMasterVMID() {
        return masterVMID;
    }

    @DataBoundSetter
    public void setMasterVMID(String masterVMID) {
        this.masterVMID = masterVMID;
    }

    public String getTag() {
        return tag;
    }

    @DataBoundSetter
    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getRemoteFS() {
        return remoteFS;
    }

    @DataBoundSetter
    public void setRemoteFS(String remoteFS) {
        this.remoteFS = remoteFS;
    }

    public int getNumberOfExecutors() {
        return numberOfExecutors;
    }

    @DataBoundSetter
    public void setNumberOfExecutors(int numberOfExecutors) {
        this.numberOfExecutors = numberOfExecutors;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getGroup() {
        return group;
    }

    @DataBoundSetter
    public void setGroup(String group) {
        this.group = group;
    }

    public String getExtraArgs() {
        return extraArgs;
    }

    @DataBoundSetter
    public void setExtraArgs(String extraArgs) {
        this.extraArgs = extraArgs;
    }

    public String getLaunchMethod() {
        return launchMethod;
    }

    @DataBoundSetter
    public void setLaunchMethod(String launchMethod) {
        this.launchMethod = launchMethod;
    }

    public String getLabel() {
        return label;
    }

    public int getSSHPort() {
        return SSHPort;
    }

    @DataBoundSetter
    public void setSSHPort(int SSHPort) {
        this.SSHPort = SSHPort;
    }

    public List<AnkaCloudSlaveTemplate.EnvironmentEntry> getEnvironments() {
        return environments;
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

    public String getJavaArgs() {
        return javaArgs;
    }

    @DataBoundSetter
    public void setJavaArgs(String javaArgs) {
        this.javaArgs = javaArgs;
    }

    public String getJnlpJenkinsOverrideUrl() {
        return jnlpJenkinsOverrideUrl;
    }

    @DataBoundSetter
    public void setJnlpJenkinsOverrideUrl(String jnlpJenkinsOverrideUrl) {
        this.jnlpJenkinsOverrideUrl = jnlpJenkinsOverrideUrl;
    }

    public String getJnlpTunnel() {
        return jnlpTunnel;
    }

    @DataBoundSetter
    public void setJnlpTunnel(String jnlpTunnel) {
        this.jnlpTunnel = jnlpTunnel;
    }

    public String getNameTemplate() {
        return nameTemplate;
    }

    @DataBoundSetter
    public void setNameTemplate(String nameTemplate) {
        this.nameTemplate = nameTemplate;
    }

    protected String masterVMID;
    protected String tag;
    protected String remoteFS = "/Users/anka/";
    protected int numberOfExecutors = 1;
    protected String credentialsId;
    protected String group;
    protected String extraArgs;
    protected String launchMethod = "ssh";

    protected String label;

    protected int SSHPort = 22;
    protected String cloudName;
    protected List<AnkaCloudSlaveTemplate.EnvironmentEntry> environments;
    protected RetentionStrategy retentionStrategy = new RunOnceCloudRetentionStrategy(1);
    protected String javaArgs;
    protected String jnlpJenkinsOverrideUrl;
    protected String jnlpTunnel;
    protected String nameTemplate;

    @DataBoundConstructor
    public DynamicSlaveProperties(String masterVMID) {
        this.masterVMID = masterVMID;

    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new DynamicSlaveStepExecution(this, context);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<DynamicSlaveProperties> {
        @Override
        public String getDisplayName() {
            return "Dynamic anka slave";
        }
    }
}
