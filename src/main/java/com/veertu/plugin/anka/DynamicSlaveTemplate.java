package com.veertu.plugin.anka;

import org.kohsuke.stapler.DataBoundConstructor;

public class DynamicSlaveTemplate extends AnkaCloudSlaveTemplate {

    private String buildId;

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    public String getBuildId() {
        return this.buildId;
    }

    @DataBoundConstructor
    public DynamicSlaveTemplate(String masterVmId) {
        super("");
        this.setSaveImageParameters(this.saveImageParameters);
        if (this.getRemoteFS() == null) {
            this.setRemoteFS("/Users/anka/");
        }
        if (this.getNumberOfExecutors() < 1) {
            this.setNumberOfExecutors(1);
        }
        if (this.getLaunchMethod() == null) {
            this.setLaunchMethod(LaunchMethod.JNLP);
        }

        this.setMasterVmId(masterVmId);
        this.setSaveImageParameters(new SaveImageParameters(false, null, null,
                false, null,false, false, false));

        this.setProperties(this);
    }



}
