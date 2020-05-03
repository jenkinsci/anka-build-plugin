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
        recurrencePeriod = TimeUnit.MINUTES.toMillis(3);
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
                LOGGER.log(Level.INFO, "Checking Anka Node {0}, instance {1}",
                        new Object[]{ankaNode.getNodeName(), ankaNode.getInstanceId()});
                AnkaCloudComputer computer = (AnkaCloudComputer) ankaNode.getComputer();
                if (computer != null && computer.isConnecting()) {
                    continue;
                }
                try {
                    if (!ankaNode.isAlive()) {
                        LOGGER.log(Level.INFO, "Anka Node {0}, instance {1}: instance is not alive - terminating",
                                new Object[]{ankaNode.getNodeName(), ankaNode.getInstanceId()});
                        ankaNode.terminate();
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING,"Anka VM failed to terminate: " + ankaNode.getInstanceId());
                    e.printStackTrace();
                }
            }
        }
    }
}
