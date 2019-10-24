package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaNotFoundException;
import hudson.model.Node;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class DynamicSlaveStepExecution extends SynchronousStepExecution<String> {


    private final DynamicSlaveProperties properties;
    private final StepContext context;

    public DynamicSlaveStepExecution(DynamicSlaveProperties dynamicSlaveProperties, StepContext context) {
        super(context);
        this.properties = dynamicSlaveProperties;
        this.context = context;
    }


    @Override
    protected String run() throws Exception {
        AnkaMgmtCloud cloud = AnkaMgmtCloud.getCloudThatHasImage(this.properties.getMasterVmId());
        if (cloud == null) {
            throw new AnkaNotFoundException("no available cloud with image " + this.properties.getMasterVmId());
        }
        String label = UUID.randomUUID().toString();
        // TODO: add retries
        AnkaOnDemandSlave slave = cloud.StartNewDynamicSlave(this.properties, label);
        slave.setMode(Node.Mode.EXCLUSIVE);
        Jenkins.getInstance().addNode(slave);
        long startTime = System.currentTimeMillis(); // fetch starting time
        while (true) {
            try {
                AnkaMgmtCloud.Log("trying to init slave %s on vm", slave.getDisplayName());
                AnkaPlannedNode.tryToCallSlave(slave);
                break;
            }
            catch (ExecutionException e) {
                if ((System.currentTimeMillis() - startTime) < slave.launchTimeout * 1000){
                    AnkaMgmtCloud.Log("caught ExecutionException when trying to start %s " +
                            "sleeping for 1 seconds to retry", slave.getNodeName());
                    Thread.sleep(1000);
                    continue;
                }
                AnkaMgmtCloud.Log("Failed to connect to slave %s", slave.getNodeName());
                slave.terminate();
                throw e;
            }
            catch (Exception e) {
                AnkaMgmtCloud.Log("vm quit unexpectedly for slave %s", slave.getNodeName());
                slave.terminate();
                throw e;
            }
        }
        return label;
    }

}
