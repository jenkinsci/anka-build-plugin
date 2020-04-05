package com.veertu.plugin.anka;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.model.listeners.RunListener;

import javax.annotation.Nonnull;
import java.io.IOException;

@Extension
public class AnkaBuildStatusListener extends RunListener<Run<?, ?>> {

    public AnkaBuildStatusListener() {
        super();
    }

    @Override
    public void onCompleted(Run<?, ?> run, @Nonnull TaskListener listener) {
        SaveImageRequestsHolder.getInstance().runFinished(run.getFullDisplayName());
        super.onCompleted(run, listener);
    }

    @Override
    public void onFinalized(Run<?, ?> run) {
        SaveImageRequestsHolder.getInstance().runFinished(run.getFullDisplayName());
        super.onFinalized(run);
    }

    @Override
    public void onInitialize(Run<?, ?> run) {
        super.onInitialize(run);
    }

    @Override
    public void onStarted(Run<?, ?> run, TaskListener listener) {
        super.onStarted(run, listener);
    }

    @Override
    public Environment setUpEnvironment(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException, Run.RunnerAbortedException {
        return super.setUpEnvironment(build, launcher, listener);
    }

    @Override
    public void onDeleted(Run<?, ?> run) {
        super.onDeleted(run);
    }

    @Override
    public void unregister() {
        super.unregister();
    }
}
