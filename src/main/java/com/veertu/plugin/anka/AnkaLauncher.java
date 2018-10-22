package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import hudson.model.TaskListener;

import com.google.common.base.Throwables;
import java.io.IOException;

public class AnkaLauncher extends AnkaCloudLauncher {

    //private DelegatingComputerLauncher launcher;
    private AnkaMgmtVm vm;

    public AnkaLauncher(AnkaMgmtVm vm, ComputerLauncher launcher) {
        super(launcher);
        this.vm = vm;
    }

    @Override
    public void waitForBoot() throws IOException, InterruptedException {
    }

    @Override
    public void launch(SlaveComputer _computer, TaskListener listener) {
        try {
            super.launch(_computer, listener);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
