package com.veertu.plugin.anka;

import com.cloudbees.plugins.credentials.domains.SchemeRequirement;
import hudson.model.Node;
import hudson.slaves.RetentionStrategy;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.List;

public class AbstractSlaveTemplate {

    public static final SchemeRequirement HTTP_SCHEME = new SchemeRequirement("http");
    public static final SchemeRequirement HTTPS_SCHEME = new SchemeRequirement("https");
    protected static final int DEFAULT_SCHEDULING_TIMEOUT = 180;
    protected AnkaNodeProperties nodeProperties;
    protected SaveImageParameters saveImageParameters;



    public String getMasterVmId() {
        return nodeProperties.getMasterVmId();
    }

    @DataBoundSetter
    public void setMasterVmId(String masterVmId) {
        this.nodeProperties.setMasterVmId(masterVmId);
    }

    public String getTag() {
        return nodeProperties.getTag();
    }

    @DataBoundSetter
    public void setTag(String tag) {
        nodeProperties.setTag(tag);
    }

    public int getLaunchDelay() {
        return nodeProperties.getLaunchDelay();
    }

    @DataBoundSetter
    public void setLaunchDelay(int launchDelay) {
        nodeProperties.setLaunchDelay(launchDelay);
    }

    public String getRemoteFS() {
        return nodeProperties.getRemoteFS();
    }

    @DataBoundSetter
    public void setRemoteFS(String remoteFS) {
        nodeProperties.setRemoteFS(remoteFS);
    }

    public String getLabelString() {
        return nodeProperties.getLabel();
    }

    @DataBoundSetter
    public void setLabelString(String labelString) {
        nodeProperties.setLabel(labelString);
    }

    public String getTemplateDescription() {
        return nodeProperties.getTemplateDescription();
    }

    @DataBoundSetter
    public void setTemplateDescription(String templateDescription) {
        nodeProperties.setTemplateDescription(templateDescription);
    }

    public int getNumberOfExecutors() {
        return nodeProperties.getNumberOfExecutors();
    }

    @DataBoundSetter
    public void setNumberOfExecutors(int numberOfExecutors) {
        nodeProperties.setNumberOfExecutors(numberOfExecutors);
    }

    public Node.Mode getMode() {
        return nodeProperties.getMode();
    }

    @DataBoundSetter
    public void setMode(Node.Mode mode) {
        this.nodeProperties.setMode(mode);
    }

    public String getCredentialsId() {
        return nodeProperties.getCredentialsId();
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        nodeProperties.setCredentialsId(credentialsId);
    }

    public String getGroup() {
        return nodeProperties.getGroup();
    }

    @DataBoundSetter
    public void setGroup(String group) {
        nodeProperties.setGroup(group);
    }

    public String getExtraArgs() {
        return nodeProperties.getExtraArgs();
    }

    @DataBoundSetter
    public void setExtraArgs(String extraArgs) {
        nodeProperties.setExtraArgs(extraArgs);
    }

    public String getLaunchMethod() {
        return nodeProperties.getLaunchMethod();
    }

    @DataBoundSetter
    public void setLaunchMethod(String launchMethod) {
        nodeProperties.setLaunchMethod(launchMethod);
    }

    public boolean isKeepAliveOnError() {
        return nodeProperties.isKeepAliveOnError();
    }

    @DataBoundSetter
    public void setKeepAliveOnError(boolean keepAliveOnError) {
        nodeProperties.setKeepAliveOnError(keepAliveOnError);
    }

    public int getSSHPort() {
        return nodeProperties.getSSHPort();
    }

    @DataBoundSetter
    public void setSSHPort(int SSHPort) {
        nodeProperties.setSSHPort(SSHPort);
    }

    public List<AnkaCloudSlaveTemplate.EnvironmentEntry> getEnvironments() {
        return nodeProperties.getEnvironments();
    }

    @DataBoundSetter
    public void setEnvironments(List<AnkaCloudSlaveTemplate.EnvironmentEntry> environments) {
        nodeProperties.setEnvironments(environments);
    }

    public RetentionStrategy getRetentionStrategy() {
        return nodeProperties.getRetentionStrategy();
    }

    @DataBoundSetter
    public void setRetentionStrategy(RetentionStrategy retentionStrategy) {
        nodeProperties.setRetentionStrategy(retentionStrategy);
    }

    public String getNameTemplate() {
        return nodeProperties.getNameTemplate();
    }

    @DataBoundSetter
    public void setNameTemplate(String nameTemplate) {
        nodeProperties.setNameTemplate(nameTemplate);
    }

    public String getJavaArgs() {
        return nodeProperties.getJavaArgs();
    }

    @DataBoundSetter
    public void setJavaArgs(String javaArgs) {
        nodeProperties.setJavaArgs(javaArgs);
    }

    public String getJnlpJenkinsOverrideUrl() {
        return nodeProperties.getJnlpJenkinsOverrideUrl();
    }

    @DataBoundSetter
    public void setJnlpJenkinsOverrideUrl(String jnlpJenkinsOverrideUrl) {
        nodeProperties.setJnlpJenkinsOverrideUrl(jnlpJenkinsOverrideUrl);
    }

    public String getJnlpTunnel() {
        return nodeProperties.getJnlpTunnel();
    }

    @DataBoundSetter
    public void setJnlpTunnel(String jnlpTunnel) {
        nodeProperties.setJnlpTunnel(jnlpTunnel);
    }

    public int getPriority() {
        return nodeProperties.getPriority();
    }

    @DataBoundSetter
    public void setPriority(int priority) {
        nodeProperties.setPriority(priority);
    }


    public SaveImageParameters getSaveImageParameters() {
        return saveImageParameters;
    }

    public Boolean getSuspend() {
        if (saveImageParameters != null) {
            return saveImageParameters.getSuspend();
        }
        return true;
    }

    @DataBoundSetter
    public void setSuspend(boolean suspend) {
        if (saveImageParameters != null) {
            saveImageParameters.setSuspend(suspend);
        }
    }

    public String getTemplateId() {
        if (saveImageParameters != null) {
            String templateId = saveImageParameters.getTemplateID();
            if (templateId != null) {
                return templateId;
            }
        }
        String masterVmId = nodeProperties.getMasterVmId();
        if (masterVmId != null) {
            return masterVmId;
        }
        return null;
    }

    @DataBoundSetter
    public void setTemplateId(String templateId) {
        if (saveImageParameters != null) {
            saveImageParameters.setTemplateID(templateId);
        }
    }

    public Boolean getSaveImage() {
        if (saveImageParameters != null) {
            return saveImageParameters.getSaveImage();
        }
        return false;
    }

    @DataBoundSetter
    public void setSaveImage(Boolean saveImage) {
        if (saveImageParameters != null) {
            saveImageParameters.setSaveImage(saveImage);
        }
    }

    public String getPushTag() {
        if (saveImageParameters != null) {
            return saveImageParameters.getTag();
        }
        return null;
    }

    @DataBoundSetter
    public void setPushTag(String tag) {
        if (saveImageParameters != null) {
            saveImageParameters.setTag(tag);
        }
    }

    public boolean isDeleteLatest() {
        if (saveImageParameters != null) {
            return saveImageParameters.isDeleteLatest();
        }
        return true;
    }

    @DataBoundSetter
    public void setDeleteLatest(boolean deleteLatest) {
        if (saveImageParameters != null) {
            saveImageParameters.setDeleteLatest(deleteLatest);
        }
    }

    public String getDescription() {
        if (saveImageParameters != null) {
            return saveImageParameters.getDescription();
        }
        return null;
    }

    @DataBoundSetter
    public void setDescription(String description) {
        if (saveImageParameters != null) {
            saveImageParameters.setDescription(description);
        }
    }

}
