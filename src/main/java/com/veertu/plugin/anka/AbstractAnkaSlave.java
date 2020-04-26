package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaVmInfo;
import com.veertu.ankaMgmtSdk.AnkaVmInstance;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Slave;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.List;

/**
 * Created by asafgur on 28/11/2016.
 */
public abstract class AbstractAnkaSlave extends Slave {

    private final AnkaMgmtCloud cloud;
    private final String instanceId;

    protected AnkaCloudSlaveTemplate template;

    public final int launchTimeout = 300;

    protected String displayName;
    protected boolean taskExecuted;
    protected boolean saveImageSent;
    protected boolean hadProblemsInBuild = false;

    public String getJobNameAndNumber() {
        return jobNameAndNumber;
    }

    public void setJobNameAndNumber(String jobNameAndNumber) {
        String finalString = jobNameAndNumber.replaceAll("\\P{Print}", "");
        this.jobNameAndNumber = finalString;

        // Update metadata with job identifier
        try {
            cloud.updateInstance(instanceId, null, null, finalString);
        } catch (AnkaMgmtException e) {
            AnkaMgmtCloud.Log("Failed to update node with job identifier");
            e.printStackTrace();
        }
    }

    protected String jobNameAndNumber;



    protected AbstractAnkaSlave(AnkaMgmtCloud cloud, String name, String nodeDescription, String remoteFS,
                                int numExecutors, Mode mode, String labelString,
                                ComputerLauncher launcher, RetentionStrategy retentionStrategy,
                                List<? extends NodeProperty<?>> nodeProperties,
                                AnkaCloudSlaveTemplate template, String instanceId) throws IOException, Descriptor.FormException {
        super(name, remoteFS, launcher);
        this.setNodeDescription(nodeDescription);
        this.setNumExecutors(numExecutors);
        this.setMode(mode);
        this.setLabelString(labelString);
        this.setRetentionStrategy(retentionStrategy);
        this.setNodeProperties(nodeProperties);
        this.instanceId = instanceId;
        this.cloud = cloud;
        this.template = template;
        this.taskExecuted = false;
        this.saveImageSent = false;

        readResolve();

    }


    public String getNodeName() {
        return this.name;
    }

    public AnkaCloudSlaveTemplate getTemplate() {
        return template;
    }


    public String getDisplayName() {
        if (this.displayName == null || this.displayName.isEmpty()) {
            return this.name;
        }
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public Computer createComputer() {
        return new AnkaCloudComputer(this, instanceId);
    }

    public void terminate() throws IOException {
        try {
            Thread.sleep(3000); // Sleep for 3 seconds to avoid those spooky ChannelClosedException
            AnkaVmInstance vm = cloud.showInstance(this.instanceId);
            if (vm != null) {
                SaveImageParameters saveImageParams = template.getSaveImageParameters();
                if (taskExecuted && saveImageParams != null && this.template.getSaveImageParameters().getSaveImage() && saveImageParams.getSaveImage() && !hadProblemsInBuild) {
                    synchronized (this) {
                        if (!this.saveImageSent) { // allow to send save image request only once
                            cloud.saveImage(this);
                            this.saveImageSent = true;
                        }
                    }
                } else {
                    cloud.terminateVMInstance(this.instanceId);
                }
            }
        } catch (AnkaMgmtException e) {
            throw new IOException(e);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (this.instanceId != null) {
                    AnkaVmInstance instance = cloud.showInstance(instanceId);
                    if (instance == null || instance.isTerminatingOrTerminated()) {
                        Jenkins.get().removeNode(this); // only agree to remove the node after the instance doesn't
                                                           // exist or is not started
                    }
                }
            } catch (AnkaMgmtException e) {
                throw new IOException(e);
            }
        }
    }

    public void setTaskExecuted(boolean didExec) {
        this.taskExecuted = didExec;
    }


    public void connected() {

    }

    public String getInstanceId() {
        return instanceId;
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

    public void setDescription(String jobAndNumber) {
        StringBuilder description = new StringBuilder();
        description.append(String.format("master image: %s,\n job name and build number: %s,\n",
                template.getMasterVmId(), jobAndNumber));
        try {
            AnkaVmInstance instance = cloud.showInstance(this.instanceId);
            description.append(String.format("Instance ID: %s \n", this.instanceId));
            description.append(String.format("Template ID: %s \n", instance.getVmId()));
            description.append(String.format("Name: %s \n", instance.getName()));
            AnkaVmInfo vmInfo = instance.getVmInfo();
            if (vmInfo != null) {
                description.append(String.format("Host: %s \n", vmInfo.getHostIp()));
                description.append(String.format("Status: %s \n", vmInfo.getStatus()));
                description.append(String.format("VM UUID: %s \n", vmInfo.getUuid()));
                description.append(String.format("VM IP: %s \n", vmInfo.getVmIp()));

            }
        } catch (AnkaMgmtException e) {
            e.printStackTrace();
        }
        super.setNodeDescription(description.toString());

    }


    public SlaveDescriptor getDescriptor() {
        return new AnkaOnDemandSlave.DescriptorImpl();
    }

    public boolean isAlive() {
        if (cloud != null) {
            try {
                AnkaVmInstance instance = cloud.showInstance(instanceId);
                if (instance != null) {
                    if (instance.isStarted()) {
                        return true;
                    }
                    if (instance.isSchedulingOrPulling()) {
                        return true;
                    }
                }
            } catch (AnkaMgmtException e) {
                return true;   // in case we can't contact the cloud, assume the VM is alive
            }
            return false;
        }
        return false;
    }

    public boolean isSchedulingOrPulling() {
        if (cloud != null) {
            try {
                AnkaVmInstance instance = cloud.showInstance(instanceId);
                if (instance != null) {
                    if (instance.isSchedulingOrPulling()) {
                        return true;
                    }
                }
            } catch (AnkaMgmtException e) {
                return true;   // in case we can't contact the cloud, return true - so caller can check again
            }
            return false;
        }
        return false;
    }

    @Extension
    public static final class DescriptorImpl extends SlaveDescriptor {

        @Override
        public String getDisplayName() {
            return "AnkaSlave";
        }

    }
}