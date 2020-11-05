package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaNotFoundException;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import java.util.UUID;


public class DynamicSlaveStepExecution extends SynchronousNonBlockingStepExecution<String> {


//    private final transient DynamicSlaveProperties properties;
    private final transient DynamicSlaveTemplate template;
    private final transient CreateDynamicAnkaNodeStep nodeStep;
    private final StepContext context;

    public DynamicSlaveStepExecution(CreateDynamicAnkaNodeStep nodeStep, StepContext context) throws Exception {
        super(context);
        this.nodeStep = nodeStep;
        this.template = nodeStep.getDynamicSlaveTemplate();
        this.context = context;
    }

    private String getBuildId(StepContext context) throws Exception {
        Run<?, ?> run = context.get(Run.class);
        if (run == null) {
            throw new Exception("Could not get build information");
        }
        return run.getExternalizableId();
    }


    @Override
    protected String run() throws Exception {

        String label = UUID.randomUUID().toString();
        String buildId = this.getBuildId(context);
        this.template.setLabel(label);
        this.template.setLabelSet(Label.parse(label));
        this.template.setSchedulingTimeout(this.nodeStep.getTimeout());
        this.template.setBuildId(buildId);
        this.template.setMode(Node.Mode.EXCLUSIVE);

        AnkaMgmtCloud cloud = AnkaMgmtCloud.getCloudThatHasImage(this.template.getMasterVmId());
        if (cloud == null) {
            throw new AnkaNotFoundException("no available cloud with image " + this.template.getMasterVmId());
        }
        cloud.addDynamicTemplate(this.template);
        return label;
    }
}
