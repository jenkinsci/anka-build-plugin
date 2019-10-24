package com.veertu.plugin.anka;

import org.kohsuke.stapler.DataBoundConstructor;

public class DynamicSlaveProperties extends AbstractSlaveTemplate {


    public AnkaCloudSlaveTemplate toSlaveTemplate() {

        AnkaCloudSlaveTemplate ankaCloudSlaveTemplate = new AnkaCloudSlaveTemplate("");
        ankaCloudSlaveTemplate.setNodeProperties(this.nodeProperties);
        ankaCloudSlaveTemplate.setRemoteFS("/Users/anka/");
        ankaCloudSlaveTemplate.setNumberOfExecutors(1);
        ankaCloudSlaveTemplate.setLaunchMethod(LaunchMethod.JNLP);
        ankaCloudSlaveTemplate.setSaveImageParameters(this.saveImageParameters);
        return ankaCloudSlaveTemplate;
    }

    public AnkaCloudSlaveTemplate toSlaveTemplate(String label) {
        this.nodeProperties.setLabel(label);
        return toSlaveTemplate();
    }

    @DataBoundConstructor
    public DynamicSlaveProperties(String masterVmId) {
        nodeProperties = new AnkaNodeProperties();
        this.nodeProperties.setMasterVmId(masterVmId);
        saveImageParameters = new SaveImageParameters(false, null, null,
                false, null,false, false);

    }



}
