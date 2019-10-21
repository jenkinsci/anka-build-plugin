package com.veertu.plugin.anka;

import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;

import javax.annotation.Nonnull;

public class DynamicSlaveStepExecution extends AbstractStepExecutionImpl {


    private final DynamicSlaveProperties properties;
    private final StepContext context;

    public DynamicSlaveStepExecution(DynamicSlaveProperties dynamicSlaveProperties, StepContext context) {
        this.properties = dynamicSlaveProperties;
        this.context = context;
    }


    @Override
    public boolean start() throws Exception {
        
        return false;
    }

    @Override
    public void stop(@Nonnull Throwable cause) throws Exception {

    }
}
