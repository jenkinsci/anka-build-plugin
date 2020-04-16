package com.veertu.plugin.anka;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.Node;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class AnkaSlaveMonitor extends AsyncPeriodicWork {

    private static final Logger LOGGER = Logger.getLogger(AnkaSlaveMonitor.class.getName());

    private final Long recurrencePeriod;

    public AnkaSlaveMonitor() {
        super("Anka Live Nodes Monitor");
        recurrencePeriod = TimeUnit.MINUTES.toMillis(5);
    }

    @Override
    public long getRecurrencePeriod() {
        return recurrencePeriod;
    }

    @Override
    protected void execute(TaskListener listener) throws IOException, InterruptedException {
        removeDeadNodes();
    }

    private void removeDeadNodes() {
        LOGGER.log(Level.INFO, "AnkaSlaveMonitor checking nodes...");

        for (Node node : Jenkins.get().getNodes()) {
            if (node instanceof AbstractAnkaSlave) {
                final AbstractAnkaSlave ankaNode = (AbstractAnkaSlave) node;
                LOGGER.info("Checking node " + ankaNode.getNodeName());
                try {
                    if (!ankaNode.isAlive()) {
                        LOGGER.info("Anka VM is not alive: " + ankaNode.getVMId());
                        ankaNode.terminate();
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING,"Anka VM failed to terminate: " + ankaNode.getVMId());
                    e.printStackTrace();
                    try {
                        Jenkins.get().removeNode(ankaNode);
                    } catch (IOException e2) {
                        LOGGER.log(Level.WARNING, "Anka VM failed to terminate: " + ankaNode.getVMId());
                        e2.printStackTrace();
                    }

                }
            }
        }
    }
}
