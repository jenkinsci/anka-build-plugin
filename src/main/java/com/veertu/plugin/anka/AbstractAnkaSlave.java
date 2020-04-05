package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.slaves.*;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.List;

/**
 * Created by asafgur on 28/11/2016.
 */
public abstract class AbstractAnkaSlave extends AbstractCloudSlave {

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
    public AbstractCloudComputer createComputer() {
        return new AnkaCloudComputer(this);
    }

    @Override
    protected void _terminate(TaskListener listener) throws IOException, InterruptedException {
        String cloudName = template.getCloudName();
        AnkaMgmtCloud cloud = (AnkaMgmtCloud) Jenkins.getInstance().getCloud(cloudName);
        if (vm != null) {
            SaveImageParameters saveImageParams = template.getSaveImageParameters();
            if (taskExecuted && saveImageParams != null && this.template.getSaveImageParameters().getSaveImage() &&
                    saveImageParams.getSaveImage() && !hadProblemsInBuild) {
                try {
                    synchronized (this) {
                        if (!this.saveImageSent) { // allow to send save image request only once
                            cloud.saveImage(this);
                            this.saveImageSent = true;
                        }
                    }
                } catch (AnkaMgmtException e) {
                    AnkaMgmtCloud.Log("Failed sending save image request. Terminating VM. Error: %s", e.getMessage());
                }
            }
            try {
                if (!this.saveImageSent)
                    vm.terminate();
            } catch (AnkaMgmtException e) {
                AnkaMgmtCloud.Log("Failed terminating vm %s. Error: %s", vm.getId(), e.getMessage());
            }
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
        String cloudName = template.getCloudName();
        AnkaMgmtCloud cloud = (AnkaMgmtCloud) Jenkins.getInstance().getCloud(cloudName);
        cloud.nodeStarted(this);
    }

    @Extension
    public static class VeertuCloudComputerListener extends ComputerListener {

        @Override
        public void preLaunch(Computer c, TaskListener taskListener) throws IOException, InterruptedException {
            super.preLaunch(c, taskListener);
        }

        public void onTemporarilyOffline(Computer c, OfflineCause cause) {
            AnkaMgmtCloud.Log("temp off");
        }

    }

    public SlaveDescriptor getDescriptor() {
        return new AnkaOnDemandSlave.DescriptorImpl();
    }

    @Extension
    public static final class DescriptorImpl extends SlaveDescriptor {

        @Override
        public String getDisplayName() {
            return "AnkaSlave";
        }

    }
}