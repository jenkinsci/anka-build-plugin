package com.veertu.plugin.anka;

import org.kohsuke.stapler.DataBoundConstructor;

public class DynamicSlaveProperties extends AbstractSlaveTemplate {


    public AnkaCloudSlaveTemplate toSlaveTemplate() {

        AnkaCloudSlaveTemplate ankaCloudSlaveTemplate = new AnkaCloudSlaveTemplate();
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
        return ankaCloudSlaveTemplate;
    }

    public AnkaCloudSlaveTemplate toSlaveTemplate(String label) {
        this.setLabel(label);
        return toSlaveTemplate();
    }

    @DataBoundConstructor
    public DynamicSlaveProperties(String masterVmId) {
        this.setMasterVmId(masterVmId);
        saveImageParameters = new SaveImageParameters(false, null, null,
                true, null, false,false, false);
    }
}
