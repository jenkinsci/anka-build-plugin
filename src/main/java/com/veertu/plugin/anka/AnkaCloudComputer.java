package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import hudson.model.*;
import hudson.slaves.SlaveComputer;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.support.steps.ExecutorStepExecution;


/**
 * Created by asafgur on 16/11/2016.
 */
public class AnkaCloudComputer extends SlaveComputer {

    private final AbstractAnkaSlave slave;
    private final String cloudName;
    private AnkaCloudSlaveTemplate template;
    protected Run<?, ?> run;
    private String vmId;

    public AnkaCloudComputer(AbstractAnkaSlave slave) {
        super(slave);
        this.slave = slave;
        this.template = slave.getTemplate();
        this.cloudName = slave.getTemplate().getCloudName();
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
            return node.getVMId();
        }
        return null;
    }

    public void connected() {
        AbstractAnkaSlave node = this.getNode();
        if (node != null) {
            node.connected();
            String vmId = node.getVMId();
            if (vmId != null) {
                this.vmId = vmId;
            }
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
        if (task instanceof ExecutorStepExecution.PlaceholderTask) {
            this.run = ((ExecutorStepExecution.PlaceholderTask) task).run();
            if (this.run != null ){
                this.slave.setDescription(this.run.getFullDisplayName());
                this.slave.setJobNameAndNumber(this.run.getFullDisplayName());
            }
        } else {
            try {
                String jobAndNumber = executor.getCurrentWorkUnit().getExecutable().toString();
                this.slave.setDescription(jobAndNumber);
                this.slave.setJobNameAndNumber(jobAndNumber);
            } catch (NullPointerException e) {
                this.slave.setDescription(executor.getDisplayName());
                this.slave.setJobNameAndNumber(executor.getDisplayName());
            }
        }
    }


    @Override
    public void taskCompleted(Executor executor, Queue.Task task, long durationMS) {
        checkLatestJobAndChangeNodeBehaviour(task);
        this.slave.setTaskExecuted(true);
        super.taskCompleted(executor, task, durationMS);

    }


    @Override
    public void taskCompletedWithProblems(Executor executor, Queue.Task task, long durationMS, Throwable problems) {
        checkLatestJobAndChangeNodeBehaviour(task);
        this.slave.setTaskExecuted(true);
        super.taskCompletedWithProblems(executor, task, durationMS, problems);
    }

    private void checkLatestJobAndChangeNodeBehaviour(Queue.Task task){

        if (this.run != null) {
            Result result = this.run.getResult();
            if (result == null && this.template.getSaveImageParameters() != null && this.template.getSaveImageParameters().getSaveImage() && this.template.getSaveImageParameters().getWaitForBuildToFinish()) {
                while (run.isBuilding()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                result = this.run.getResult();
            }
            if (result == Result.FAILURE || result == Result.ABORTED || result == Result.UNSTABLE) {
                this.slave.setHadErrorsOnBuild(true);
            }
            return;
        }

        // check the latest build and report back to slave object , in case keepAliveOnerror is set
        if (!(task instanceof AbstractProject)) {
            return;
        }
        AbstractBuild b = ((AbstractProject)task).getLastBuild();
        if (b == null) {
            return;
        }
        ResultTrend trend = ResultTrend.getResultTrend(b);
        AnkaMgmtCloud.Log("slave: %s, vm id: %s exited build with trend: %s", this.slave.getNodeName(),
                this.slave.getVM().getId(), trend.toString());
        switch (trend){
            case NOT_BUILT :
            case FAILURE :
            case STILL_FAILING :
            case NOW_UNSTABLE:
            case STILL_UNSTABLE :
            case UNSTABLE :
            case ABORTED :
                this.slave.setHadErrorsOnBuild(true);
                break;
            case SUCCESS :
            case FIXED :
                this.slave.setHadErrorsOnBuild(false);
        }
    }

    public String getCloudName() {
        return cloudName;
    }
}
