package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaNotFoundException;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

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
        cloud.createDynamicTemplate(this.properties, label, this.nodeStep.getTimeout());
        return label;
    }
}
