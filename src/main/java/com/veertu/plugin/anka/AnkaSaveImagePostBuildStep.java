package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.ankaMgmtSdk.exceptions.SaveImageRequestIdMissingException;
import com.veertu.plugin.anka.exceptions.SaveImageStatusTimeout;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;


public class AnkaSaveImagePostBuildStep extends Recorder {

    private static final int DEFAULT_TIMEOUT_MINS = 120;

    private boolean shouldFail;
    private int timeoutMinutes = DEFAULT_TIMEOUT_MINS;

    @DataBoundConstructor
    public AnkaSaveImagePostBuildStep(boolean shouldFail, int timeoutMinutes) {
        this.shouldFail = shouldFail;
        if (timeoutMinutes <= 0)
            this.timeoutMinutes = DEFAULT_TIMEOUT_MINS;
        else
            this.timeoutMinutes = timeoutMinutes;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener taskListener) throws InterruptedException, IOException{
        boolean isSuccess = true;

        taskListener.getLogger().print("Checking save image status...");
        AbstractAnkaSlave slave = (AbstractAnkaSlave) (build.getBuiltOn());

        // Killing vm to manually initiate save image request
        assert slave != null;
        slave.setTaskExecuted(true);
        slave.terminate();

        try {
            isSuccess = ImageSaver.isSuccessful(slave.getJobNameAndNumber(), timeoutMinutes);
            taskListener.getLogger().println(isSuccess? "Done!" : "Failed!");
        } catch (SaveImageStatusTimeout e) {
            taskListener.getLogger().println("TIMED OUT");
            isSuccess = false;
        }
        catch (SaveImageRequestIdMissingException e) {
            taskListener.getLogger().print(e.getMessage());
            isSuccess = false;
        }
        catch (AnkaMgmtException e) {
            taskListener.getLogger().print("error while checking if save image requess finished\n");
            e.printStackTrace();
            isSuccess = false;
        }
        if (!shouldFail) {
            return true;
        }
        return isSuccess;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public boolean shouldFail() {
        return shouldFail;
    }

    public int getTimeoutMinutes() {
        return timeoutMinutes;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Anka save image";
        }

        public int getTimeoutMinutes() {
            return DEFAULT_TIMEOUT_MINS;
        }

    }

}
