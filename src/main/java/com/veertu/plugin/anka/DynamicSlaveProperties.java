package com.veertu.plugin.anka;

import hudson.model.Node;
import org.kohsuke.stapler.DataBoundConstructor;

public class DynamicSlaveProperties extends AbstractSlaveTemplate {

    private int schedulingTimeout;

    public AnkaCloudSlaveTemplate toSlaveTemplate() {

        AnkaCloudSlaveTemplate ankaCloudSlaveTemplate = new AnkaCloudSlaveTemplate("");
        ankaCloudSlaveTemplate.setProperties(this);
        ankaCloudSlaveTemplate.setSaveImageParameters(this.saveImageParameters);
        if (this.getRemoteFS() == null) {
            ankaCloudSlaveTemplate.setRemoteFS("/Users/anka/");
        }
        if (this.getNumberOfExecutors() < 1) {
            ankaCloudSlaveTemplate.setNumberOfExecutors(1);
        }
        if (this.getLaunchMethod() == null) {
            ankaCloudSlaveTemplate.setLaunchMethod(LaunchMethod.JNLP);
        }

        ankaCloudSlaveTemplate.setSchedulingTimeout(this.schedulingTimeout);

        return ankaCloudSlaveTemplate;
    }

    public AnkaCloudSlaveTemplate toSlaveTemplate(String label, int schedulingTimeout) {
        this.setLabel(label);
        this.schedulingTimeout = schedulingTimeout;
        return toSlaveTemplate();
    }

    @DataBoundConstructor
    public DynamicSlaveProperties(String masterVmId) {
        this.setMasterVmId(masterVmId);
        saveImageParameters = new SaveImageParameters(false, null, null,
                false, null,false, false);

    }



}
