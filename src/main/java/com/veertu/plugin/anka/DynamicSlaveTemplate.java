package com.veertu.plugin.anka;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
    @SuppressFBWarnings(value = "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR",
            justification = "Inherited template setters/getters are intentionally used to apply dynamic defaults during construction.")
    public DynamicSlaveTemplate(String masterVmId) {
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
