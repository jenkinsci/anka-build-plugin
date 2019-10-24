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

    private final DynamicSlaveProperties dynamicSlaveProperties;


    public String getMasterVmId() {
        return dynamicSlaveProperties.getMasterVmId();
    }

    @DataBoundSetter
    public void setMasterVmId(String masterVmId) {
        this.dynamicSlaveProperties.setMasterVmId(masterVmId);
    }

    public String getTag() {
        return dynamicSlaveProperties.getTag();
    }

    @DataBoundSetter
    public void setTag(String tag) {
        dynamicSlaveProperties.setTag(tag);
    }

    public int getLaunchDelay() {
        return dynamicSlaveProperties.getLaunchDelay();
    }

    @DataBoundSetter
    public void setLaunchDelay(int launchDelay) {
        dynamicSlaveProperties.setLaunchDelay(launchDelay);
    }

    public String getRemoteFS() {
        return dynamicSlaveProperties.getRemoteFS();
    }

    @DataBoundSetter
    public void setRemoteFS(String remoteFS) {
        dynamicSlaveProperties.setRemoteFS(remoteFS);
    }

    public String getLabelString() {
        return dynamicSlaveProperties.getLabelString();
    }

    @DataBoundSetter
    public void setLabelString(String labelString) {
        dynamicSlaveProperties.setLabelString(labelString);
    }

    public String getTemplateDescription() {
        return dynamicSlaveProperties.getTemplateDescription();
    }

    @DataBoundSetter
    public void setTemplateDescription(String templateDescription) {
        dynamicSlaveProperties.setTemplateDescription(templateDescription);
    }

    public int getNumberOfExecutors() {
        return dynamicSlaveProperties.getNumberOfExecutors();
    }

    @DataBoundSetter
    public void setNumberOfExecutors(int numberOfExecutors) {
        dynamicSlaveProperties.setNumberOfExecutors(numberOfExecutors);
    }

    public Node.Mode getMode() {
        return dynamicSlaveProperties.getMode();
    }

    @DataBoundSetter
    public void setMode(Node.Mode mode) {
        this.dynamicSlaveProperties.setMode(mode);
    }

    public String getCredentialsId() {
        return dynamicSlaveProperties.getCredentialsId();
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        dynamicSlaveProperties.setCredentialsId(credentialsId);
    }

    public String getGroup() {
        return dynamicSlaveProperties.getGroup();
    }

    @DataBoundSetter
    public void setGroup(String group) {
        dynamicSlaveProperties.setGroup(group);
    }

    public String getExtraArgs() {
        return dynamicSlaveProperties.getExtraArgs();
    }

    @DataBoundSetter
    public void setExtraArgs(String extraArgs) {
        dynamicSlaveProperties.setExtraArgs(extraArgs);
    }

    public String getLaunchMethod() {
        return dynamicSlaveProperties.getLaunchMethod();
    }

    @DataBoundSetter
    public void setLaunchMethod(String launchMethod) {
        dynamicSlaveProperties.setLaunchMethod(launchMethod);
    }

    public boolean isKeepAliveOnError() {
        return dynamicSlaveProperties.isKeepAliveOnError();
    }

    @DataBoundSetter
    public void setKeepAliveOnError(boolean keepAliveOnError) {
        dynamicSlaveProperties.setKeepAliveOnError(keepAliveOnError);
    }

    public int getSSHPort() {
        return dynamicSlaveProperties.getSSHPort();
    }

    @DataBoundSetter
    public void setSSHPort(int SSHPort) {
        dynamicSlaveProperties.setSSHPort(SSHPort);
    }

    public List<AnkaCloudSlaveTemplate.EnvironmentEntry> getEnvironments() {
        return dynamicSlaveProperties.getEnvironments();
    }

    @DataBoundSetter
    public void setEnvironments(List<AnkaCloudSlaveTemplate.EnvironmentEntry> environments) {
        dynamicSlaveProperties.setEnvironments(environments);
    }

    public RetentionStrategy getRetentionStrategy() {
        return dynamicSlaveProperties.getRetentionStrategy();
    }

    @DataBoundSetter
    public void setRetentionStrategy(RetentionStrategy retentionStrategy) {
        dynamicSlaveProperties.setRetentionStrategy(retentionStrategy);
    }

    public String getNameTemplate() {
        return dynamicSlaveProperties.getNameTemplate();
    }

    @DataBoundSetter
    public void setNameTemplate(String nameTemplate) {
        dynamicSlaveProperties.setNameTemplate(nameTemplate);
    }

    public String getJavaArgs() {
        return dynamicSlaveProperties.getJavaArgs();
    }

    @DataBoundSetter
    public void setJavaArgs(String javaArgs) {
        dynamicSlaveProperties.setJavaArgs(javaArgs);
    }

    public String getJnlpJenkinsOverrideUrl() {
        return dynamicSlaveProperties.getJnlpJenkinsOverrideUrl();
    }

    @DataBoundSetter
    public void setJnlpJenkinsOverrideUrl(String jnlpJenkinsOverrideUrl) {
        dynamicSlaveProperties.setJnlpJenkinsOverrideUrl(jnlpJenkinsOverrideUrl);
    }

    public String getJnlpTunnel() {
        return dynamicSlaveProperties.getJnlpTunnel();
    }

    @DataBoundSetter
    public void setJnlpTunnel(String jnlpTunnel) {
        dynamicSlaveProperties.setJnlpTunnel(jnlpTunnel);
    }

    public int getPriority() {
        return dynamicSlaveProperties.getPriority();
    }

    @DataBoundSetter
    public void setPriority(int priority) {
        dynamicSlaveProperties.setPriority(priority);
    }
    

    public Boolean getSuspend() {
        return dynamicSlaveProperties.getSuspend();
    }

    @DataBoundSetter
    public void setSuspend(boolean suspend) {
        dynamicSlaveProperties.setSuspend(suspend);
    }

    public String getTemplateId() {
        return dynamicSlaveProperties.getTemplateId();
    }

    @DataBoundSetter
    public void setTemplateId(String templateId) {
        dynamicSlaveProperties.setTemplateId(templateId);
    }

    public Boolean getSaveImage() {
        return dynamicSlaveProperties.getSaveImage();
    }

    @DataBoundSetter
    public void setSaveImage(Boolean saveImage) {
        dynamicSlaveProperties.setSaveImage(saveImage);
    }

    public String getPushTag() {
        return dynamicSlaveProperties.getTag();
    }

    @DataBoundSetter
    public void setPushTag(String tag) {
        dynamicSlaveProperties.setTag(tag);
    }

    public boolean isDeleteLatest() {
        return dynamicSlaveProperties.isDeleteLatest();
    }

    @DataBoundSetter
    public void setDeleteLatest(boolean deleteLatest) {
        dynamicSlaveProperties.setDeleteLatest(deleteLatest);
    }

    public String getDescription() {
        return dynamicSlaveProperties.getDescription();
    }

    @DataBoundSetter
    public void setDescription(String description) {
        dynamicSlaveProperties.setDescription(description);
    }


    @DataBoundConstructor
    public CreateDynamicAnkaNodeStep(String masterVmId) {
        this.dynamicSlaveProperties = new DynamicSlaveProperties(masterVmId);
    }

    public DynamicSlaveProperties getDynamicSlaveProperties() {
        return dynamicSlaveProperties;
    }

    public StepExecution start(StepContext context) throws Exception {
        return new DynamicSlaveStepExecution(this.dynamicSlaveProperties, context);
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
            if (!(context instanceof AccessControlled ? (AccessControlled) context : Jenkins.getInstance()).hasPermission(Computer.CONFIGURE)) {
                return new ListBoxModel();
            }
            final List<StandardUsernameCredentials> credentials = CredentialsProvider.lookupCredentials(StandardUsernameCredentials.class,
                    context, ACL.SYSTEM, DynamicSlaveProperties.HTTP_SCHEME, DynamicSlaveProperties.HTTPS_SCHEME);
            return new StandardUsernameListBoxModel().withAll(credentials);
        }
    }

}
