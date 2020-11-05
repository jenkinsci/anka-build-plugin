package com.veertu.plugin.anka;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.slaves.iterators.api.NodeIterator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.veertu.plugin.anka.AnkaMgmtCloud.getAnkaClouds;
import static hudson.model.Run.fromExternalizableId;

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
        super("Anka Monitor");
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
        cleanDynamicTemplates();
    }

    private void cleanDynamicTemplates() {
        LOGGER.log(Level.INFO, "AnkaSlaveMonitor cleaning dynamic templates...");
        List<AnkaMgmtCloud> clouds = getAnkaClouds();
        for (AnkaMgmtCloud cloud : clouds) {
            List<DynamicSlaveTemplate> templatesToRemove = new ArrayList<>();
            List<DynamicSlaveTemplate> dynamicTemplates = cloud.getDynamicTemplates();
            for (DynamicSlaveTemplate template : dynamicTemplates) {
                try {
                    String jobId = template.getBuildId();
                    if (jobId.equals("")) {
                        LOGGER.log(Level.WARNING, "dynamic template with label {0} has no build id assigned",
                                new Object[]{template.getLabel()});
                        continue;
                    }
                    Run r = fromExternalizableId(jobId);
                    if (r == null || !r.isBuilding() ) {
                        throw new Exception("catch me");
                    }
                } catch (Exception e) {
                    templatesToRemove.add(template);
                }
            }
            if (templatesToRemove.size() > 0) {
                for (DynamicSlaveTemplate t : templatesToRemove) {
                    LOGGER.log(Level.INFO, "AnkaSlaveMonitor clearing dynamic template {0} from cloud {1}",
                            new Object[]{t.getLabel(), cloud.getCloudName()});
                    cloud.removeDynamicTemplate(t);
                }
            }
        }
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
