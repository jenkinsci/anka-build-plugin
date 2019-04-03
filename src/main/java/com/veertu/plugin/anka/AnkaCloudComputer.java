package com.veertu.plugin.anka;

import hudson.model.*;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.AbstractCloudSlave;

/**
 * Created by asafgur on 16/11/2016.
 */
public class AnkaCloudComputer extends AbstractCloudComputer {

    private final AnkaOnDemandSlave slave;
    private String uuid;
    private AnkaCloudSlaveTemplate template;

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
        String jobAndNumber = executor.getCurrentWorkUnit().getExecutable().toString();
        this.slave.setDescription(jobAndNumber);
    }


    @Override
    public void taskCompleted(Executor executor, Queue.Task task, long durationMS) {
        checkLatestJobAndChangeNodeBehaviour(task);
        super.taskCompleted(executor, task, durationMS);

    }


    @Override
    public void taskCompletedWithProblems(Executor executor, Queue.Task task, long durationMS, Throwable problems) {
        checkLatestJobAndChangeNodeBehaviour(task);
        super.taskCompletedWithProblems(executor, task, durationMS, problems);
    }

    private void checkLatestJobAndChangeNodeBehaviour(Queue.Task task){
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
                this.slave.setHadErrorsOnBuild(true);
                break;
            case SUCCESS :
            case FIXED :
            case ABORTED :
                this.slave.setHadErrorsOnBuild(false);
        }
    }


}
