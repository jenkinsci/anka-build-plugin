package com.veertu.plugin.anka;

import hudson.Extension;
import hudson.model.*;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.ankaMgmtSdk.exceptions.SaveImageRequestIdMissingException;
import com.veertu.plugin.anka.exceptions.SaveImageStatusTimeout;

public class AnkaSaveImageBuildStep extends Step {

    public final int DEFAULT_TIMEOUT_MINS = 120;

    private final boolean shouldFail;
    private final int timeoutMinutes;

    @DataBoundConstructor
    public AnkaSaveImageBuildStep(boolean shouldFail, int timeoutMinutes) {
        this.shouldFail = shouldFail;
        if (timeoutMinutes <= 0)
            this.timeoutMinutes = DEFAULT_TIMEOUT_MINS;
        else
            this.timeoutMinutes = timeoutMinutes;
    }

    public boolean getShouldFail() {
        return shouldFail;
    }

    public int getTimeoutMinutes() {
        return timeoutMinutes;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new AnkaSaveImageBuildStep.Execution( this, context);
    }

    public static class Execution extends SynchronousNonBlockingStepExecution<Boolean> {
        private final boolean shouldFail;
        private final int timeoutMinutes;
        private final StepContext context;


        Execution(AnkaSaveImageBuildStep step, StepContext context) {
            super(context);
            this.shouldFail = step.getShouldFail();
            this.timeoutMinutes = step.getTimeoutMinutes();
            this.context = context;
        }

        @Override
        protected Boolean run() throws Exception {

            Run<?, ?> run = context.get(Run.class);
            if (run == null) {
                return true;
            }
            String buildId = run.getFullDisplayName();
            TaskListener listener = context.get(TaskListener.class);
            boolean isSuccess = true;

            assert listener != null;
            listener.getLogger().print("Checking save image status...");

            try {
                isSuccess = ImageSaver.isSuccessful(buildId, timeoutMinutes);
                listener.getLogger().println(isSuccess? "Done!" : "Failed!");
            } catch (SaveImageStatusTimeout e) {
                listener.getLogger().println("TIMED OUT");
                isSuccess = false;
            } catch (SaveImageRequestIdMissingException e) {
                listener.getLogger().println(e.getMessage());
                isSuccess = false;
            } catch (AnkaMgmtException e) {
                listener.getLogger().println("ERROR!");
                listener.getLogger().println(e.getMessage());
                e.printStackTrace();
                return false;
            }

            if (shouldFail && !isSuccess)
                run.setResult(Result.FAILURE);
            
            return isSuccess;
        }
    }


        @Extension
    public static final class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "ankaGetSaveImageResult";
        }

        @Override
        public String getDisplayName() {
            return "Wait for previous save image requests results";
        }

		@Override
		public Set<? extends Class<?>> getRequiredContext() {
            Set<Class<?>> set = new HashSet<>();
            set.add(Run.class);
            set.add(TaskListener.class);

            return Collections.unmodifiableSet(set);

		}
    }
}