package com.veertu.plugin.anka;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.slaves.ComputerListener;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class AnkaCloudComputerListener extends ComputerListener {

    private static final transient Logger LOGGER = Logger.getLogger(AnkaCloudComputerListener.class.getName());


    @Override
    public void onOnline(Computer c, TaskListener listener) {
        if (c instanceof AnkaCloudComputer) {
            ((AnkaCloudComputer) c).connected();
        }
    }

    @Override
    public void onLaunchFailure(Computer c, TaskListener taskListener) throws IOException, InterruptedException {
        if (c instanceof AnkaCloudComputer) {
            AnkaCloudComputer computer = (AnkaCloudComputer)c;
            if (c.isLaunchSupported()) {
                LOGGER.log(Level.INFO, "Computer {0}, instance {1} failed to launch, terminating",
                        new Object[]{computer.getName(), computer.getVMId()});
                computer.terminate();
            }
        }
    }

}
