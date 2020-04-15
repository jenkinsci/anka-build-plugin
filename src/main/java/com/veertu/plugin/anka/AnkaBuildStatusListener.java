package com.veertu.plugin.anka;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import javax.annotation.Nonnull;

@Extension
public class AnkaBuildStatusListener extends RunListener<Run<?, ?>> {

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


}
