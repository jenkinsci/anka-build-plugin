package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import hudson.model.*;
import hudson.slaves.SlaveComputer;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.support.steps.ExecutorStepExecution;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Created by asafgur on 16/11/2016.
 */
public class AnkaCloudComputer extends SlaveComputer {
    private static final Logger LOGGER = Logger.getLogger(AnkaCloudComputer.class.getName());
    enum BuildOutcome {
        SUCCESS,
        FAILURE,
        UNKNOWN
    }

    private final AbstractAnkaSlave slave;
    private final String cloudName;
    private AnkaCloudSlaveTemplate template;
    protected Run<?, ?> run;
    private RunIdentity acceptedRunIdentity;
    private final String vmId;
    private boolean afterFirstConnection = false;
    private boolean launching;

    public AnkaCloudComputer(AbstractAnkaSlave slave, String vmId) {
        super(slave);
        this.slave = slave;
        this.template = slave.getTemplate();
        this.cloudName = slave.getTemplate().getCloudName();
        this.vmId = vmId;
    }

    @Override
    public boolean isConnecting() {
        boolean parentConnecting = super.isConnecting();
        if (parentConnecting) {
            return true;
        }
        if (isSchedulingOrPulling()) {
            return true;
        }
        if (isAlive() && launching) {
            return true;
        }
        return false;
    }

    public boolean afterFirstConnection() {
        return afterFirstConnection;
    }

    public void firstConnectionAttempted() {
        afterFirstConnection = true;
    }

    @Override
    public AbstractAnkaSlave getNode() {
        return (AbstractAnkaSlave) super.getNode();
    }

    public String getVMId() {
        if (vmId != null) {
            return vmId;
        }
        AbstractAnkaSlave node = getNode();
        if (node != null) {
            return node.getInstanceId();
        }
        return null;
    }
    
    public boolean isAlive() {
        AbstractAnkaSlave node = getNode();
        if (node != null) {
            return node.isAlive();
        }
        return false;
    }

    public void connected() {
        AbstractAnkaSlave node = this.getNode();
        if (node != null) {
            node.connected();
        }
    }

    public void onRemoved() {
        AnkaMgmtCloud.Log("Computer %s removed", this.getName());
        if (vmId != null) {
            String cloudName = template.getCloudName();
            AnkaMgmtCloud cloud = (AnkaMgmtCloud) Jenkins.get().getCloud(cloudName);
            if (cloud != null) {
                try {
                    cloud.terminateVMInstance(vmId);
                } catch (AnkaMgmtException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public AnkaCloudSlaveTemplate getTemplate() {
        return template;
    }
    
    @Override
    public void taskAccepted(Executor executor, Queue.Task task) {
        super.taskAccepted(executor, task);
        this.run = null;
        this.acceptedRunIdentity = null;
        this.slave.setHadUnknownBuildOutcome(false);
        if (task instanceof ExecutorStepExecution.PlaceholderTask) {
            this.run = ((ExecutorStepExecution.PlaceholderTask) task).run();
            if (this.run != null ){
                this.slave.setDescription(this.run.getFullDisplayName());
                this.slave.setJobNameAndNumber(this.run.getFullDisplayName());
                this.acceptedRunIdentity = RunIdentity.fromRun(this.run);
            }
        } else {
            String jobAndNumber;
            try {
                jobAndNumber = executor.getCurrentWorkUnit().getExecutable().toString();
                this.slave.setDescription(jobAndNumber);
                this.slave.setJobNameAndNumber(jobAndNumber);
            } catch (NullPointerException e) {
                jobAndNumber = executor.getDisplayName();
                this.slave.setDescription(jobAndNumber);
                this.slave.setJobNameAndNumber(jobAndNumber);
            }
            if (task instanceof Job) {
                this.acceptedRunIdentity = parseRunIdentity((Job<?, ?>) task, jobAndNumber);
            } else {
                this.acceptedRunIdentity = parseRunIdentityFromDisplayName(jobAndNumber);
            }
        }
        this.slave.taskAccepted(executor, task);
    }


    @Override
    public void taskCompleted(Executor executor, Queue.Task task, long durationMS) {
        this.slave.taskCompleted(executor, task, durationMS);
        try {
            safelyCheckLatestJobAndChangeNodeBehaviour(executor, task);
        } finally {
            super.taskCompleted(executor, task, durationMS);
        }

    }


    @Override
    public void taskCompletedWithProblems(Executor executor, Queue.Task task, long durationMS, Throwable problems) {
        this.slave.taskCompleted(executor, task, durationMS);
        try {
            safelyCheckLatestJobAndChangeNodeBehaviour(executor, task);
        } finally {
            super.taskCompletedWithProblems(executor, task, durationMS, problems);
        }
    }

    void safelyCheckLatestJobAndChangeNodeBehaviour(Executor executor, Queue.Task task) {
        try {
            checkLatestJobAndChangeNodeBehaviour(executor, task);
        } catch (RuntimeException e) {
            // Unknown/unstable metadata should never prevent run-once cleanup.
            applyBuildOutcome(BuildOutcome.UNKNOWN);
            LOGGER.log(Level.WARNING,
                    AnkaLog.prefix(
                    String.format("Computer %s, instance %s failed to resolve build result for task %s",
                            getName(), getVMId(), task)),
                    e);
        }
    }

    private void checkLatestJobAndChangeNodeBehaviour(Executor executor, Queue.Task task){

        if (this.run != null) {
            updateBuildOutcome(this.run, "pipeline run reference");
            return;
        }

        Run<?, ?> completedRun = getCompletedRun(executor, task);
        if (completedRun == null) {
            applyBuildOutcome(BuildOutcome.UNKNOWN);
            AnkaMgmtCloud.Log("slave: %s, vm id: %s exited build with unknown outcome (no completed run found)",
                    this.slave.getNodeName(), this.slave.getInstanceId());
            return;
        }

        updateBuildOutcome(completedRun, "completed run lookup");
    }

    private Run<?, ?> getCompletedRun(Executor executor, Queue.Task task) {
        if (executor != null) {
            Queue.Executable executable = executor.getCurrentExecutable();
            if (executable instanceof Run) {
                return (Run<?, ?>) executable;
            }
        }

        Run<?, ?> runFromAcceptedIdentity = getRunFromAcceptedIdentity(task);
        if (runFromAcceptedIdentity != null) {
            return runFromAcceptedIdentity;
        }

        if (this.acceptedRunIdentity != null) {
            return null;
        }

        if (task instanceof Job) {
            return ((Job<?, ?>) task).getLastCompletedBuild();
        }

        if (task instanceof AbstractProject) {
            return ((AbstractProject<?, ?>) task).getLastCompletedBuild();
        }

        return null;
    }

    private Run<?, ?> getRunFromAcceptedIdentity(Queue.Task task) {
        if (this.acceptedRunIdentity == null) {
            return null;
        }

        Job<?, ?> job = resolveJobForIdentity(task, this.acceptedRunIdentity.getJobFullName());
        if (job == null) {
            return null;
        }

        return job.getBuildByNumber(this.acceptedRunIdentity.getBuildNumber());
    }

    private Job<?, ?> resolveJobForIdentity(Queue.Task task, String jobFullName) {
        if (task instanceof Job) {
            Job<?, ?> job = (Job<?, ?>) task;
            if (job.getFullName().equals(jobFullName)) {
                return job;
            }
        }

        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins == null) {
            return null;
        }

        return jenkins.getItemByFullName(jobFullName, Job.class);
    }

    private void updateBuildOutcome(Run<?, ?> completedRun, String source) {
        Result result = resolveResult(completedRun);
        BuildOutcome buildOutcome = resolveBuildOutcome(result);
        applyBuildOutcome(buildOutcome);
        switch (buildOutcome) {
            case UNKNOWN:
                AnkaMgmtCloud.Log("slave: %s, vm id: %s exited build with UNKNOWN outcome via %s",
                        this.slave.getNodeName(), this.slave.getInstanceId(), source);
                break;
            case SUCCESS:
                AnkaMgmtCloud.Log("slave: %s, vm id: %s exited build with SUCCESS result via %s",
                        this.slave.getNodeName(), this.slave.getInstanceId(), source);
                break;
            case FAILURE:
                AnkaMgmtCloud.Log("slave: %s, vm id: %s exited build with FAILURE result %s via %s",
                        this.slave.getNodeName(), this.slave.getInstanceId(), result, source);
                break;
        }
    }

    private Result resolveResult(Run<?, ?> completedRun) {
        Result result = completedRun.getResult();
        if (result != null) {
            return result;
        }

        SaveImageParameters saveImageParameters = this.template.getSaveImageParameters();
        if (saveImageParameters != null && saveImageParameters.getSaveImage() && saveImageParameters.getWaitForBuildToFinish()) {
            while (completedRun.isBuilding()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            return completedRun.getResult();
        }

        return null;
    }

    private void applyBuildOutcome(BuildOutcome buildOutcome) {
        this.slave.setHadErrorsOnBuild(shouldMarkBuildAsErrorForKeepAlive(buildOutcome));
        this.slave.setHadUnknownBuildOutcome(buildOutcome == BuildOutcome.UNKNOWN);
    }

    static BuildOutcome resolveBuildOutcome(Result result) {
        if (result == null) {
            return BuildOutcome.UNKNOWN;
        }
        if (result == Result.SUCCESS) {
            return BuildOutcome.SUCCESS;
        }
        return BuildOutcome.FAILURE;
    }

    static boolean shouldMarkBuildAsErrorForKeepAlive(BuildOutcome buildOutcome) {
        return buildOutcome == BuildOutcome.FAILURE;
    }

    static RunIdentity parseRunIdentityFromDisplayName(String jobDisplayName) {
        if (jobDisplayName == null) {
            return null;
        }

        int runSeparator = jobDisplayName.lastIndexOf(" #");
        if (runSeparator <= 0 || runSeparator >= jobDisplayName.length() - 2) {
            return null;
        }

        String jobName = jobDisplayName.substring(0, runSeparator).trim();
        String buildNumberText = jobDisplayName.substring(runSeparator + 2).trim();
        if (jobName.isEmpty()) {
            return null;
        }

        try {
            int buildNumber = Integer.parseInt(buildNumberText);
            if (buildNumber <= 0) {
                return null;
            }
            return new RunIdentity(jobName, buildNumber);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static RunIdentity parseRunIdentity(Job<?, ?> job, String jobDisplayName) {
        RunIdentity runIdentityFromDisplayName = parseRunIdentityFromDisplayName(jobDisplayName);
        if (runIdentityFromDisplayName == null) {
            return null;
        }

        return new RunIdentity(job.getFullName(), runIdentityFromDisplayName.getBuildNumber());
    }

    static final class RunIdentity {
        private final String jobFullName;
        private final int buildNumber;

        private RunIdentity(String jobFullName, int buildNumber) {
            this.jobFullName = jobFullName;
            this.buildNumber = buildNumber;
        }

        static RunIdentity fromRun(Run<?, ?> run) {
            return new RunIdentity(run.getParent().getFullName(), run.getNumber());
        }

        String getJobFullName() {
            return jobFullName;
        }

        int getBuildNumber() {
            return buildNumber;
        }
    }

    public String getCloudName() {
        return cloudName;
    }

    public boolean isSchedulingOrPulling() {
        AbstractAnkaSlave node = getNode();
        if (node != null) {
            return node.isSchedulingOrPulling();
        }
        return false;
    }

    public void terminate() throws IOException {
        try {
            // first try to terminate through the node
            AbstractAnkaSlave node = getNode();
            if (node != null) {
                node.terminate();
                return;
            }
            // if the node doesn't exist terminate directly with the cloud
            if (vmId != null) {
                String cloudName = template.getCloudName();
                AnkaMgmtCloud cloud = (AnkaMgmtCloud) Jenkins.get().getCloud(cloudName);
                if (cloud != null) {
                    cloud.terminateVMInstance(vmId);
                }
            }
        } catch (AnkaMgmtException e) {
            throw new IOException(e);
        }
    }

    public void reportLaunching() {
        this.launching = true;
    }

    public void reportLaunchFinished() {
        this.launching = false;
    }
}
