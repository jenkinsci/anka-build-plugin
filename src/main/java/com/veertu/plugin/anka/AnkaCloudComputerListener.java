package com.veertu.plugin.anka;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.slaves.ComputerListener;

@Extension
public class AnkaCloudComputerListener extends ComputerListener {

    @Override
    public void onOnline(Computer c, TaskListener listener) {
        if (c instanceof AnkaCloudComputer) {
            ((AnkaCloudComputer) c).connected();
        }
    }
}
