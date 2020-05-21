package com.veertu.plugin.anka;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import jenkins.slaves.iterators.api.NodeIterator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class AnkaSlaveMonitor extends AsyncPeriodicWork {

    private static final Logger LOGGER = Logger.getLogger(AnkaSlaveMonitor.class.getName());
    private static int monitorRecurrenceMinutes = 10;
    private static List<AnkaSlaveMonitor> monitors = new ArrayList<>();

    public static void register(AnkaSlaveMonitor monitor) {
        monitors.add(monitor);
    }

    public static void recurrenceChanged() {
        for (AnkaSlaveMonitor monitor: monitors) {
            monitor.resetRecurrence();
        }
    }

    public static int getMonitorRecurrenceMinutes() {
        return monitorRecurrenceMinutes;
    }

    private Long recurrencePeriod;

    public AnkaSlaveMonitor() {
        super("Anka Live Nodes Monitor");
        recurrencePeriod = TimeUnit.MINUTES.toMillis(monitorRecurrenceMinutes);
        register(this);
    }

    public void resetRecurrence() {
        recurrencePeriod = TimeUnit.MINUTES.toMillis(monitorRecurrenceMinutes);
    }

    public static void setMonitorRecurrenceMinutes(int minutes) {
        if (minutes < 1) { // allow a minimum of 1 minute
            minutes = 1;
        }
        monitorRecurrenceMinutes = minutes;
        recurrenceChanged();
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
        for (AbstractAnkaSlave ankaNode : NodeIterator.nodes(AbstractAnkaSlave.class)) {
            LOGGER.log(Level.FINE, "Checking Anka Node {0}, instance {1}",
                    new Object[]{ankaNode.getNodeName(), ankaNode.getInstanceId()});
            AnkaCloudComputer computer = (AnkaCloudComputer) ankaNode.getComputer();
            if (computer != null && computer.isConnecting()) {
                continue;
            }
            try {
                if (!ankaNode.isAlive()) {
                    LOGGER.log(Level.WARNING, "Anka Node {0}, instance {1}: instance is not alive - terminating",
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
