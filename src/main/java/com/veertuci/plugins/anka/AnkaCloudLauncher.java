package com.veertuci.plugins.anka;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.DelegatingComputerLauncher;
import hudson.slaves.SlaveComputer;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by asafgur on 16/11/2016.
 */
public class AnkaCloudLauncher extends DelegatingComputerLauncher {
    private static final Logger LOGGER = Logger.getLogger(AnkaCloudLauncher.class.getName());

    public AnkaCloudLauncher(ComputerLauncher launcher) {
        super(launcher);
    }

    @Override
    public boolean isLaunchSupported() {
        return true;
    } // always good to be positive

    @Override
    public void launch(SlaveComputer slaveComputer, TaskListener listener) throws IOException, InterruptedException {
        super.launch(slaveComputer, listener); // this is delegated to another computer launcher

    }

    public void waitForBoot() throws InterruptedException, IOException, AnkaMgmtException {}

    @Override
    public Descriptor<ComputerLauncher> getDescriptor() {
        // Don't allow creation of launcher from UI
        throw new UnsupportedOperationException();
    }

    public ComputerLauncher getDelegate() {
        return super.getLauncher();
    }
}
