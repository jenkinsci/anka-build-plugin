package com.veertu.plugin.anka;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.listeners.RunListener;

@Extension
public class AnkaBuildStatusListener extends RunListener<Run<?, ?>> {


    @Override
    public void onFinalized(Run<?, ?> run) {
        SaveImageRequestsHolder.getInstance().runFinished(run.getFullDisplayName());
        super.onFinalized(run);
    }


}
