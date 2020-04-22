package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaNotFoundException;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import hudson.model.Node;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import java.io.IOException;
import java.util.UUID;


public class DynamicSlaveStepExecution extends SynchronousNonBlockingStepExecution<String> {


    private final transient DynamicSlaveProperties properties;
    private final transient CreateDynamicAnkaNodeStep nodeStep;

    public DynamicSlaveStepExecution(CreateDynamicAnkaNodeStep nodeStep, StepContext context) {
        super(context);
        this.nodeStep = nodeStep;
        this.properties = nodeStep.getDynamicSlaveProperties();
    }


    @Override
    protected String run() throws Exception {
        AnkaMgmtCloud cloud = AnkaMgmtCloud.getCloudThatHasImage(this.properties.getMasterVmId());
        if (cloud == null) {
            throw new AnkaNotFoundException("no available cloud with image " + this.properties.getMasterVmId());
        }
        String label = UUID.randomUUID().toString();
        AnkaOnDemandSlave slave = null;
        int timeoutMillis = this.nodeStep.getTimeout() * 1000;
        long startTime = System.currentTimeMillis();
        int round = 1;
        while (true) {
            try {
                slave = cloud.StartNewDynamicSlave(this.properties, label);
                break;
            } catch (InterruptedException | IOException | AnkaMgmtException e) {
                if ((System.currentTimeMillis() - startTime) < timeoutMillis){
                    Thread.sleep(1000 * (round * round ));
                    round++;
                    continue;
                }
                throw e;
            }

        }
        slave.setMode(Node.Mode.EXCLUSIVE);

        return label;
    }

}
