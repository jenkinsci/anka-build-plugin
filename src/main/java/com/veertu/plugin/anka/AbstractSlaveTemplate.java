package com.veertu.plugin.anka;

import com.cloudbees.plugins.credentials.domains.SchemeRequirement;
import hudson.model.Node;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.ArrayList;
import java.util.List;

public class AbstractSlaveTemplate {

    public static final SchemeRequirement HTTP_SCHEME = new SchemeRequirement("http");
    public static final SchemeRequirement HTTPS_SCHEME = new SchemeRequirement("https");
    protected static final int DEFAULT_SCHEDULING_TIMEOUT = 1800;
    protected String masterVmId;
    protected String tag;
    protected int launchDelay;
    protected String remoteFS;
    protected String labelString;
    protected String templateDescription;
    protected int numberOfExecutors;
    protected Node.Mode mode;
    protected String credentialsId;
    protected String group;
    protected String extraArgs;
    protected String launchMethod;
    protected boolean keepAliveOnError;
    protected int SSHPort = 22;
    protected List<AnkaCloudSlaveTemplate.EnvironmentEntry> environments;
    protected RetentionStrategy retentionStrategy;
    protected String nameTemplate;
    protected String javaArgs;
    protected String jnlpJenkinsOverrideUrl;
    protected String jnlpTunnel;
    protected int priority;
    protected SaveImageParameters saveImageParameters;
    protected String cloudName;
    protected int instanceCapacity;



    protected int idleMinutes = 1;


    public AbstractSlaveTemplate() {
        this.retentionStrategy = new RunOnceCloudRetentionStrategy(idleMinutes);
    }

    public int getIdleMinutes() {
        return idleMinutes;
    }

    @DataBoundSetter
    public void setIdleMinutes(int idleMinutes) {
        this.idleMinutes = idleMinutes;
    }

    public int getInstanceCapacity() {
        return instanceCapacity;
    }

    @DataBoundSetter
    public void setInstanceCapacity(int instanceCapacity) {
        this.instanceCapacity = instanceCapacity;
    }



    public String getCloudName() {
        return cloudName;
    }

    public String getDisplayName() {
        return labelString;
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
        return valOrNull(labelString);
    }

    public String getLabelString() {
        return getLabel();
    }



    @DataBoundSetter
    public void setLabel(String labelString) {
        this.labelString = labelString;
    }

    @Deprecated
    @DataBoundSetter
    public void setLabelString(String labelString) {
        this.labelString = labelString;
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

    public boolean getKeepAliveOnError() {
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
        try {
            return ((RunOnceCloudRetentionStrategy) retentionStrategy).clone();
        } catch (CloneNotSupportedException e) {
            if (idleMinutes < 1) {
                idleMinutes = 1;
            }
            return new RunOnceCloudRetentionStrategy(this.idleMinutes);
        }
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
    public void setSuspend(Boolean suspend) {
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
        String masterVmId = getMasterVmId();
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

    public boolean isAppendTimestamp() {
        if (saveImageParameters != null) {
            return saveImageParameters.isAppendTimestamp();
        }
        return false; // default behavior is to have the timestamp since this is the current behavior 15.10.2020 (asaf)
    }

    public boolean getDontAppendTimestamp() {
        if (saveImageParameters != null) {
            return saveImageParameters.getDontAppendTimestamp();
        }
        return true; // default behavior is to have the timestamp since this is the current behavior 15.10.2020 (asaf)
    }



    @DataBoundSetter
    public void setDontAppendTimestamp(Boolean appendTimestamp) {
        if (saveImageParameters != null) {
            saveImageParameters.setDontAppendTimestamp(appendTimestamp);
        }
    }

    public boolean isDeleteLatest() {
        if (saveImageParameters != null) {
            return saveImageParameters.isDeleteLatest();
        }
        return true;
    }

    @DataBoundSetter
    public void setDeleteLatest(Boolean deleteLatest) {
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

    public Boolean getWaitForBuildToFinish() {
        if (saveImageParameters != null) {
            return saveImageParameters.getWaitForBuildToFinish();
        }
        return false;
    }

    @DataBoundSetter
    public void setWaitForBuildToFinish(Boolean wait) {
        if (saveImageParameters != null) {
            saveImageParameters.setWaitForBuildToFinish(wait);
        }
    }

    public List<? extends NodeProperty<?>> getNodeProperties() {
        ArrayList<EnvironmentVariablesNodeProperty.Entry> a = new ArrayList<EnvironmentVariablesNodeProperty.Entry>();
        for (AnkaCloudSlaveTemplate.EnvironmentEntry e :this.getEnvironments()) {
            a.add(new EnvironmentVariablesNodeProperty.Entry(e.name, e.value));
        }

        EnvironmentVariablesNodeProperty env = new EnvironmentVariablesNodeProperty(a);
        ArrayList<NodeProperty<?>> props = new ArrayList<>();
        props.add(env);
        return props;
    }

    protected void setProperties(AbstractSlaveTemplate slave) {
        setMasterVmId(slave.getMasterVmId());
        setTag(slave.getTag());
        setLaunchDelay(slave.getLaunchDelay());
        setRemoteFS(slave.getRemoteFS());
        setLabel(slave.getLabel());
        setTemplateDescription(slave.getTemplateDescription());
        setNumberOfExecutors(slave.getNumberOfExecutors());
        setMode(slave.getMode());
        setCredentialsId(slave.getCredentialsId());
        setGroup(slave.getGroup());
        setExtraArgs(slave.getExtraArgs());
        setLaunchMethod(slave.getLaunchMethod());
        setKeepAliveOnError(slave.getKeepAliveOnError());
        setSSHPort(slave.getSSHPort());
        setEnvironments(slave.getEnvironments());
        setRetentionStrategy(slave.getRetentionStrategy());
        setNameTemplate(slave.getNameTemplate());
        setJavaArgs(slave.getJavaArgs());
        setJnlpJenkinsOverrideUrl(slave.getJnlpJenkinsOverrideUrl());
        setJnlpTunnel(slave.getJnlpTunnel());
        setPriority(slave.getPriority());
        setSuspend(slave.getSuspend());
        setTemplateId(slave.getTemplateId());
        setSaveImage(slave.getSaveImage());
        setPushTag(slave.getPushTag());
        setDontAppendTimestamp(slave.getDontAppendTimestamp());
        setDeleteLatest(slave.isDeleteLatest());
        setDescription(slave.getDescription());
        setWaitForBuildToFinish(slave.getWaitForBuildToFinish());

    }

}
