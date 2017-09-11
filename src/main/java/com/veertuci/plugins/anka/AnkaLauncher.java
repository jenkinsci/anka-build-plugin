package com.veertuci.plugins.anka;

import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertuci.plugins.AnkaMgmtCloud;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.DelegatingComputerLauncher;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.TaskListener;

import com.google.common.base.Throwables;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AnkaLauncher extends AnkaCloudLauncher {

    private DelegatingComputerLauncher launcher;
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
            launcher.launch(_computer, listener);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}