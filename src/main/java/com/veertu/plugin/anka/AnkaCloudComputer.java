package com.veertu.plugin.anka;

import hudson.model.*;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.AbstractCloudSlave;
import org.apache.tools.ant.taskdefs.Sleep;
import org.jenkinsci.plugins.workflow.support.steps.ExecutorStepExecution;


/**
 * Created by asafgur on 16/11/2016.
 */
public class AnkaCloudComputer extends AbstractCloudComputer {

    private final AnkaOnDemandSlave slave;
    private String uuid;
    private AnkaCloudSlaveTemplate template;
    protected Run<?, ?> run;

    public AnkaCloudComputer(AbstractCloudSlave slave) {
        super(slave);
        this.slave = (AnkaOnDemandSlave)slave;
        AnkaOnDemandSlave slaveComputer = (AnkaOnDemandSlave) slave;
        this.template = slaveComputer.getTemplate();
    }

    public String getUuid() {
        return uuid;
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
            }
        } else {
            try {
                String jobAndNumber = executor.getCurrentWorkUnit().getExecutable().toString();
                this.slave.setDescription(jobAndNumber);
            } catch (NullPointerException e) {
                this.slave.setDescription(executor.getDisplayName());
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
            if (result == null && this.template.getSaveImageParameters() != null && this.template.getSaveImageParameters().getWaitForBuildToFinish()) {
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


}
