package com.veertu.plugin.anka;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import hudson.Extension;
import hudson.model.*;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.slaves.RetentionStrategy;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



public class CreateDynamicAnkaNodeStep extends Step {

    private final DynamicSlaveTemplate dynamicSlaveTemplate;
    private int timeout = 1200;


    public String getMasterVmId() {
        return dynamicSlaveTemplate.getMasterVmId();
    }

    @DataBoundSetter
    public void setMasterVmId(String masterVmId) {
        this.dynamicSlaveTemplate.setMasterVmId(masterVmId);
    }

    public String getCloudName() {
        return dynamicSlaveTemplate.getCloudName();
    }

    @DataBoundSetter
    public void setCloudName(String cloudName) {
        this.dynamicSlaveTemplate.setCloudName(cloudName);
    }

    public String getTag() {
        return dynamicSlaveTemplate.getTag();
    }

    @DataBoundSetter
    public void setTag(String tag) {
        dynamicSlaveTemplate.setTag(tag);
    }

    public int getLaunchDelay() {
        return dynamicSlaveTemplate.getLaunchDelay();
    }

    @DataBoundSetter
    public void setLaunchDelay(int launchDelay) {
        dynamicSlaveTemplate.setLaunchDelay(launchDelay);
    }

    public String getRemoteFS() {
        return dynamicSlaveTemplate.getRemoteFS();
    }

    @DataBoundSetter
    public void setRemoteFS(String remoteFS) {
        dynamicSlaveTemplate.setRemoteFS(remoteFS);
    }

    public String getLabelString() {
        return dynamicSlaveTemplate.getLabel();
    }

    @DataBoundSetter
    public void setLabelString(String labelString) {
        dynamicSlaveTemplate.setLabel(labelString);
    }

    public String getTemplateDescription() {
        return dynamicSlaveTemplate.getTemplateDescription();
    }

    @DataBoundSetter
    public void setTemplateDescription(String templateDescription) {
        dynamicSlaveTemplate.setTemplateDescription(templateDescription);
    }

    public int getNumberOfExecutors() {
        return dynamicSlaveTemplate.getNumberOfExecutors();
    }

    @DataBoundSetter
    public void setNumberOfExecutors(int numberOfExecutors) {
        dynamicSlaveTemplate.setNumberOfExecutors(numberOfExecutors);
    }

    public Node.Mode getMode() {
        return dynamicSlaveTemplate.getMode();
    }

    @DataBoundSetter
    public void setMode(Node.Mode mode) {
        this.dynamicSlaveTemplate.setMode(mode);
    }

    public String getCredentialsId() {
        return dynamicSlaveTemplate.getCredentialsId();
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        dynamicSlaveTemplate.setCredentialsId(credentialsId);
    }

    public String getGroup() {
        return dynamicSlaveTemplate.getGroup();
    }

    @DataBoundSetter
    public void setGroup(String group) {
        dynamicSlaveTemplate.setGroup(group);
    }

    public String getExtraArgs() {
        return dynamicSlaveTemplate.getExtraArgs();
    }

    @DataBoundSetter
    public void setExtraArgs(String extraArgs) {
        dynamicSlaveTemplate.setExtraArgs(extraArgs);
    }

    public String getLaunchMethod() {
        return dynamicSlaveTemplate.getLaunchMethod();
    }

    @DataBoundSetter
    public void setLaunchMethod(String launchMethod) {
        dynamicSlaveTemplate.setLaunchMethod(launchMethod);
    }

    public boolean isKeepAliveOnError() {
        return dynamicSlaveTemplate.isKeepAliveOnError();
    }

    @DataBoundSetter
    public void setKeepAliveOnError(boolean keepAliveOnError) {
        dynamicSlaveTemplate.setKeepAliveOnError(keepAliveOnError);
    }

    public int getSSHPort() {
        return dynamicSlaveTemplate.getSSHPort();
    }

    @DataBoundSetter
    public void setSSHPort(int SSHPort) {
        dynamicSlaveTemplate.setSSHPort(SSHPort);
    }

    public List<AnkaCloudSlaveTemplate.EnvironmentEntry> getEnvironments() {
        return dynamicSlaveTemplate.getEnvironments();
    }

    @DataBoundSetter
    public void setEnvironments(List<AnkaCloudSlaveTemplate.EnvironmentEntry> environments) {
        dynamicSlaveTemplate.setEnvironments(environments);
    }

    public RetentionStrategy getRetentionStrategy() {
        return dynamicSlaveTemplate.getRetentionStrategy();
    }

    @DataBoundSetter
    public void setRetentionStrategy(RetentionStrategy retentionStrategy) {
        dynamicSlaveTemplate.setRetentionStrategy(retentionStrategy);
    }

    public String getNameTemplate() {
        return dynamicSlaveTemplate.getNameTemplate();
    }

    @DataBoundSetter
    public void setNameTemplate(String nameTemplate) {
        dynamicSlaveTemplate.setNameTemplate(nameTemplate);
    }

    public String getJavaArgs() {
        return dynamicSlaveTemplate.getJavaArgs();
    }

    @DataBoundSetter
    public void setJavaArgs(String javaArgs) {
        dynamicSlaveTemplate.setJavaArgs(javaArgs);
    }

    public String getJnlpJenkinsOverrideUrl() {
        return dynamicSlaveTemplate.getJnlpJenkinsOverrideUrl();
    }

    @DataBoundSetter
    public void setJnlpJenkinsOverrideUrl(String jnlpJenkinsOverrideUrl) {
        dynamicSlaveTemplate.setJnlpJenkinsOverrideUrl(jnlpJenkinsOverrideUrl);
    }

    public String getJnlpTunnel() {
        return dynamicSlaveTemplate.getJnlpTunnel();
    }

    @DataBoundSetter
    public void setJnlpTunnel(String jnlpTunnel) {
        dynamicSlaveTemplate.setJnlpTunnel(jnlpTunnel);
    }

    public int getPriority() {
        return dynamicSlaveTemplate.getPriority();
    }

    @DataBoundSetter
    public void setPriority(int priority) {
        dynamicSlaveTemplate.setPriority(priority);
    }

    public int getVcpu(int vcpu) {
        return dynamicSlaveTemplate.getVcpu();
    }

    @DataBoundSetter
    public void setVcpu(int vcpu) {
        dynamicSlaveTemplate.setVcpu(vcpu);
    }
    
    public int getVram(int vram) {
        return dynamicSlaveTemplate.getVram();
    }

    @DataBoundSetter
    public void setVram(int vram) {
        dynamicSlaveTemplate.setVram(vram);
    }

    public Boolean getSuspend() {
        return dynamicSlaveTemplate.getSuspend();
    }

    @DataBoundSetter
    public void setSuspend(boolean suspend) {
        dynamicSlaveTemplate.setSuspend(suspend);
    }

    public String getTemplateId() {
        return dynamicSlaveTemplate.getTemplateId();
    }

    @DataBoundSetter
    public void setTemplateId(String templateId) {
        dynamicSlaveTemplate.setTemplateId(templateId);
    }

    public Boolean getSaveImage() {
        return dynamicSlaveTemplate.getSaveImage();
    }

    @DataBoundSetter
    public void setSaveImage(Boolean saveImage) {
        dynamicSlaveTemplate.setSaveImage(saveImage);
    }

    public String getPushTag() {
        return dynamicSlaveTemplate.getPushTag();
    }

    @DataBoundSetter
    public void setPushTag(String tag) {
        dynamicSlaveTemplate.setPushTag(tag);
    }

    public boolean isAppendTimestamp() {
        return dynamicSlaveTemplate.isAppendTimestamp();
    }

    @DataBoundSetter
    public void setDontAppendTimestamp(boolean dontAppendTimestamp) {
        dynamicSlaveTemplate.setDontAppendTimestamp(dontAppendTimestamp);
    }

    public boolean isDeleteLatest() {
        return dynamicSlaveTemplate.isDeleteLatest();
    }

    @DataBoundSetter
    public void setDeleteLatest(boolean deleteLatest) {
        dynamicSlaveTemplate.setDeleteLatest(deleteLatest);
    }

    public String getDescription() {
        return dynamicSlaveTemplate.getDescription();
    }

    @DataBoundSetter
    public void setDescription(String description) {
        dynamicSlaveTemplate.setDescription(description);
    }

    @DataBoundSetter
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getTimeout() { return timeout; }

    @DataBoundConstructor
    public CreateDynamicAnkaNodeStep(String masterVmId) {
        this.dynamicSlaveTemplate = new DynamicSlaveTemplate(masterVmId);
    }

    public DynamicSlaveTemplate getDynamicSlaveTemplate() {
        return dynamicSlaveTemplate;
    }

    public StepExecution start(StepContext context) throws Exception {
        return new DynamicSlaveStepExecution(this, context);
    }

    @Extension
    public static final class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            Set<Class<?>> set = new HashSet<>();
            set.add(Run.class);
            set.add(TaskListener.class);

            return Collections.unmodifiableSet(set);
        }

        @Override
        public String getFunctionName() {
            return "createDynamicAnkaNode";
        }

        @Override
        public String getDisplayName() {
            return "create dynamic anka node";

        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context) {
            if (!(context instanceof AccessControlled ? (AccessControlled) context : Jenkins.get()).hasPermission(Computer.CONFIGURE)) {
                return new ListBoxModel();
            }
            final List<StandardUsernameCredentials> credentials = CredentialsProvider.lookupCredentials(StandardUsernameCredentials.class,
                    context, ACL.SYSTEM, DynamicSlaveTemplate.HTTP_SCHEME, DynamicSlaveTemplate.HTTPS_SCHEME);
            StandardUsernameListBoxModel listBox = new StandardUsernameListBoxModel();
            for (StandardUsernameCredentials cred: credentials) {
                listBox.with(cred);
            }
            return listBox;
        }
    }

}
