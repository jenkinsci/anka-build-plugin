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

        if (this.template.getCloudName() != null) {
            if (this.template.getCloudName().isEmpty()) {
                throw new Exception("cloudName can not be empty");
            }

            AnkaMgmtCloud ankaCloud = AnkaMgmtCloud.get(this.template.getCloudName());
            if (ankaCloud == null) {
                throw new AnkaNotFoundException(
                        String.format("Anka cloud \"%s\" doesn't exist: ",
                                this.template.getCloudName())
                );
            }

            if (!ankaCloud.hasMasterVm(this.template.getMasterVmId())) {
                throw new AnkaNotFoundException(
                        String.format("Anka cloud \"%s\" doesn't have vm with id \"%s\"",
                                this.template.getCloudName(), this.template.getMasterVmId())
                );
            }

            ankaCloud.addDynamicTemplate(this.template);
            return label;
        }

        AnkaMgmtCloud ankaCloud = AnkaMgmtCloud.getCloudThatHasImage(this.template.getMasterVmId());
        if (ankaCloud == null) {
            throw new AnkaNotFoundException(
                    String.format("none of the Anka clouds has vm with id \"%s\"",
                            this.template.getMasterVmId()));
        }

        this.template.setCloudName(ankaCloud.getCloudName());
        ankaCloud.addDynamicTemplate(this.template);

        return label;
    }
}
