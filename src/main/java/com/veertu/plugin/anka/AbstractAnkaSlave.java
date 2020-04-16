package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
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

    protected boolean hadProblemsInBuild = false;

    protected AnkaCloudSlaveTemplate template;
    protected AnkaMgmtVm vm;
    public final int launchTimeout = 300;
    protected String displayName;
    protected boolean taskExecuted;
    protected boolean saveImageSent;

    public String getJobNameAndNumber() {
        return jobNameAndNumber;
    }

    public void setJobNameAndNumber(String jobNameAndNumber) {

        this.jobNameAndNumber = jobNameAndNumber.replaceAll("\\P{Print}", "");
    }

    protected String jobNameAndNumber;

    protected static final int launchTimeoutSeconds = 2000;
    protected static final int maxNumRetries = 5;
    protected static final int retryWaitTime = 100;

    protected AbstractAnkaSlave(String name, String nodeDescription, String remoteFS,
                                int numExecutors, Mode mode, String labelString,
                                ComputerLauncher launcher, RetentionStrategy retentionStrategy,
                                List<? extends NodeProperty<?>> nodeProperties,
                                AnkaCloudSlaveTemplate template, AnkaMgmtVm vm) throws Descriptor.FormException, IOException {
        super(name, nodeDescription, remoteFS, numExecutors, mode,
                labelString, launcher,
                retentionStrategy, nodeProperties);
        this.name = name;
        this.template = template;
        this.vm = vm;
        this.taskExecuted = false;
        this.saveImageSent = false;
        readResolve();
    }

    public AbstractAnkaSlave(String name, String nodeDescription, String remoteFS, String numExecutors,
                             Mode mode, String labelString, ComputerLauncher launcher,
                             RetentionStrategy retentionStrategy, List<? extends NodeProperty<?>> nodeProperties) throws IOException, Descriptor.FormException {
        super(name, nodeDescription, remoteFS, numExecutors, mode, labelString,
                launcher, retentionStrategy, nodeProperties);
        this.name = name;
        this.taskExecuted = false;

    }



    public AnkaCloudSlaveTemplate getTemplate() {
        return template;
    }

    public AnkaMgmtVm getVM() {
        return vm;
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
        return new AnkaCloudComputer(this, vm.getId());
    }

    public void terminate() throws IOException {
        String cloudName = template.getCloudName();
        AnkaMgmtCloud cloud = (AnkaMgmtCloud) Jenkins.get().getCloud(cloudName);
        if (vm != null) {
            SaveImageParameters saveImageParams = template.getSaveImageParameters();
            if (taskExecuted && saveImageParams != null && this.template.getSaveImageParameters().getSaveImage() && saveImageParams.getSaveImage() && !hadProblemsInBuild) {
                try {
                    synchronized (this) {
                        if (!this.saveImageSent) { // allow to send save image request only once
                            cloud.saveImage(this);
                            this.saveImageSent = true;
                        }
                        Jenkins.get().removeNode(this);
                    }
                } catch (AnkaMgmtException e) {
                    throw new IOException(e);
                }
            } else {
                try {
                    cloud.terminateVMInstance(vm.getId());
                    Jenkins.get().removeNode(this);
                } catch (AnkaMgmtException e) {
                    AnkaMgmtCloud.Log("Failed to terminate node %s", this.name);
                    throw new IOException(e);
                }
            }
        } else {
            Jenkins.get().removeNode(this);  // in case we don't have a vm remove this node
        }
    }

    public void setTaskExecuted(boolean didExec) {
        this.taskExecuted = didExec;
    }

    protected void setVM(AnkaMgmtVm vm) {
        this.vm = vm;
    }

    public void register() throws IOException {
        Jenkins.getInstance().addNode(this);
    }

    public void connected() {

    }

    public String getVMId() {
        if (this.vm != null) {
            return this.vm.getId();
        }
        return null;
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
        String description = String.format("master image: %s, job name and build number: %s, vm info: (%s)",
                template.getMasterVmId(), jobAndNumber, this.vm.getInfo());
        super.setNodeDescription(description);

    }


    public SlaveDescriptor getDescriptor() {
        return new AnkaOnDemandSlave.DescriptorImpl();
    }

    public boolean isAlive() {
        String vmId = vm.getId();
        if (vmId == null) {
            return false;
        }
        String cloudName = template.getCloudName();
        AnkaMgmtCloud cloud = (AnkaMgmtCloud) Jenkins.get().getCloud(cloudName);
        if (cloud != null) {
            try {
                AnkaVmInstance instance = cloud.showInstance(vmId);
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
        String vmId = vm.getId();
        if (vmId == null) {
            return false;
        }
        String cloudName = template.getCloudName();
        AnkaMgmtCloud cloud =  (AnkaMgmtCloud) Jenkins.get().getCloud(cloudName);
        if (cloud != null) {
            try {
                AnkaVmInstance instance = cloud.showInstance(vmId);
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