package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.slaves.*;
import jenkins.model.Jenkins;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
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
    public AbstractCloudComputer createComputer() {
        return new AnkaCloudComputer(this);
    }

    @Override
    protected void _terminate(TaskListener listener) throws IOException, InterruptedException {
        if (vm != null) {
            SaveImageParameters saveImageParams = template.getSaveImageParameters();
            if (taskExecuted && saveImageParams != null && saveImageParams.getSaveImage() && !hadProblemsInBuild) {
                try {
                    synchronized (this) {
                        if (!this.saveImageSent) { // allow to send save image request only once
                            String cloudName = template.getCloudName();
                            AnkaMgmtCloud cloud = (AnkaMgmtCloud) Jenkins.getInstance().getCloud(cloudName);

                            ImageSaver.saveImage(cloud, this, vm);
                            SaveImageSentEvent.saveImageSent(this.getNodeName());
                            this.saveImageSent = true;
                        }
                    }
                } catch (AnkaMgmtException e) {
                    throw new IOException(e);
                }
            } else {
                NodeTerminatedEvent.nodeTerminated(this.getNodeName());
                vm.terminate();
            }
        }
    }

    public void setTaskExecuted(boolean didExec) {
        this.taskExecuted = didExec;
    }

    protected void setVM(AnkaMgmtVm vm) {
        this.vm = vm;
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

        public void onOffline(@Nonnull Computer c, @CheckForNull OfflineCause cause) {
            try {
                AnkaOnDemandSlave node = (AnkaOnDemandSlave) c.getNode();
                // if we got here - that means this is an Anka node

                if (cause == null) {
                    // assume that this is a restart
                    AnkaMgmtCloud.Log("computer %s not disconnecting, restart assumed", c.getName());
                    return;
                }

                AnkaMgmtCloud.Log("computer %s started onOffline hook", c.getName());
                if (node == null) {
                    AnkaMgmtCloud.Log("computer %s node is null returning", c.getName());
                    NodeTerminatedEvent.nodeTerminated(c.getName());
                    return;
                }




                AnkaMgmtCloud.Log("node %s is still alive, handling", node.getNodeName());
                int maxRetries = 20;
                int sleepTime = 200000;
                int retries = 0;

                while (true) {
                    try {
                        if (node.getVM().isRunning()) {

                            AnkaMgmtCloud.Log("node %s vm exists, terminating", node.getNodeName());
                            if (node.canTerminate()) {
                                node.terminate();
                            }
                            break;
                        } else {
                            AnkaMgmtCloud.Log("node %s vm does not exist, removing node from jenkins", node.getNodeName());
                            Jenkins.getInstance().removeNode(node);
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        AnkaMgmtCloud.Log("node %s got %s exception while terminating",
                                node.getNodeName(), e.getClass().toString());
                        if (retries < maxRetries) {
                            AnkaMgmtCloud.Log("node %s retries %d", node.getNodeName(), retries);
                            retries++;
                            try {
                                Thread.sleep(sleepTime);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                                continue;
                            }
                            continue;
                        }
                        AnkaMgmtCloud.Log("node %s retries exhausted", node.getNodeName());
                        break;
                    } catch (InterruptedException e) {
                        AnkaMgmtCloud.Log("node %s termination interrupted", node.getNodeName());
                        e.printStackTrace();
                    }
                }
            } catch (ClassCastException e) {
                return;
            }
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