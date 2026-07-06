package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaVmInfo;
import com.veertu.ankaMgmtSdk.AnkaVmInstance;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.slaves.NodeProvisioner;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by asafgur on 16/11/2016.
 */
public class AnkaPlannedNodeCreator {

    private static int connectionAttemps = 10;

    private static final transient Logger LOGGER = Logger.getLogger(AnkaPlannedNodeCreator.class.getName());


    public static NodeProvisioner.PlannedNode createPlannedNode(final AnkaMgmtCloud cloud, final AnkaCloudSlaveTemplate template, final AbstractAnkaSlave slave) {
        // Reserve this node as in-progress capacity the moment it is provisioned. It stays reserved
        // across the whole boot -> connect -> dispatch window and is released only when it accepts a
        // task (AnkaCloudComputer.taskAccepted) or is removed (AnkaCloudComputer.onRemoved). Holding
        // it that long -- rather than releasing when it merely connects -- is what stops a
        // connected-but-not-yet-dispatched node from being re-provisioned as a duplicate.
        final String nodeName = slave.getNodeName();
        cloud.reserveInstanceInProgress(template, nodeName);
        boolean submitted = false;
        try {
            Future<Node> future = Computer.threadPoolForRemoting.submit(() -> {
                Node node = null;
                try {
                    node = waitAndConnect(cloud, template, slave);
                    return node;
                } catch (InterruptedException interruptedException) {
                    terminateProvisioningInstance(cloud, slave, interruptedException);
                    Thread.currentThread().interrupt();
                    return null;
                } finally {
                    // Only release here when the launch failed/aborted (node == null): that node will
                    // never accept a task, so free its reservation now instead of waiting for removal.
                    // On success we intentionally keep the reservation until taskAccepted/onRemoved.
                    if (node == null) {
                        cloud.releaseInstanceInProgress(nodeName);
                    }
                }
            });
            submitted = true;
            return new NodeProvisioner.PlannedNode(template.getDisplayName(), future, template.getNumberOfExecutors());
        } finally {
            if (!submitted) {
                cloud.releaseInstanceInProgress(nodeName);
            }
        }
    }

    private static void markFirstConnectionAttempted(AbstractAnkaSlave slave) {
        AnkaCloudComputer ankaComputer = (AnkaCloudComputer) slave.toComputer();
        if (ankaComputer != null) {
            ankaComputer.firstConnectionAttempted();
        }
    }

    private static void terminateProvisioningInstance(AnkaMgmtCloud cloud, AbstractAnkaSlave slave, InterruptedException interruptedException) {
        String instanceId = slave.getInstanceId();
        LOGGER.log(Level.WARNING, AnkaLog.prefix("Provisioning interrupted for instance {0}, sending termination"), instanceId);
        try {
            cloud.terminateVMInstance(instanceId, slave);
        } catch (AnkaMgmtException terminateError) {
            LOGGER.log(Level.WARNING, AnkaLog.prefix("Failed to terminate interrupted instance " + instanceId), terminateError);
        } finally {
            LOGGER.log(Level.FINE, AnkaLog.prefix("Provisioning interruption stacktrace for instance " + instanceId), interruptedException);
        }
    }

    public static Node waitAndConnect(final AnkaMgmtCloud cloud, final AnkaCloudSlaveTemplate template, final AbstractAnkaSlave slave) throws AnkaMgmtException, InterruptedException {
        final long timeStarted = System.currentTimeMillis();
        int consecutiveLookupMisses = 0;
        while (true) {
            String instanceId = slave.getInstanceId();
            int vmCheckTime = cloud.getVmPollTime();
            AnkaVmInstance instance = cloud.showInstance(instanceId);
            if (instance == null) {
                // Even with a cache-aware showInstance(), the controller can still be
                // briefly inconsistent under burst load. Abandoning on the first miss
                // leaves the VM to boot and connect on its own, producing an orphaned
                // node that never ran a job and that RunOnceCloudRetentionStrategy
                // refuses to reap (afterFirstConnection stays false). Retry a bounded
                // number of times before giving up.
                consecutiveLookupMisses++;
                if (consecutiveLookupMisses >= connectionAttemps) {
                    LOGGER.log(Level.WARNING, AnkaLog.prefix("instance `{0}` not found in cloud {1} after {2} attempts, terminating provisioning"),
                            new Object[]{instanceId, cloud.getCloudName(), consecutiveLookupMisses});
                    markFirstConnectionAttempted(slave);
                    cloud.terminateVMInstance(instanceId);
                    return null;
                }
                LOGGER.log(Level.WARNING, AnkaLog.prefix("instance `{0}` not found in cloud {1} (attempt {2}/{3}), retrying"),
                        new Object[]{instanceId, cloud.getCloudName(), consecutiveLookupMisses, connectionAttemps});
                Thread.sleep(vmCheckTime);
                continue;
            }
            consecutiveLookupMisses = 0;

            if (instance.isStarted()) {
                AnkaVmInfo vmInfo = instance.getVmInfo();
                if (vmInfo == null) { // shouldn't happen if vm is Started
                    Thread.sleep(vmCheckTime);
                    continue;
                }
                String hostIp = vmInfo.getHostIp();
                if (hostIp == null || hostIp.isEmpty()) { // the node doesn't have an ip yet
                    Thread.sleep(vmCheckTime);
                    continue;
                }
                try {
                    Computer computer = slave.toComputer();
                    if ( computer != null ) {
                        AnkaCloudComputer ankaComputer = (AnkaCloudComputer) computer;
                        ankaComputer.connect(false);
                    }
                } finally {
                    markFirstConnectionAttempted(slave);
                }

                return slave;

            }

            if (instance.isPulling()) {
                Thread.sleep(vmCheckTime);
                continue;
            }

            if (instance.isScheduling()) {
                final long sinceStarted = System.currentTimeMillis() - timeStarted;
                int schedulingTimeout = template.getSchedulingTimeout();
                long schedulingTimeoutMillis = TimeUnit.SECONDS.toMillis(schedulingTimeout);
                LOGGER.log(Level.FINE, AnkaLog.prefix("Instance {0} is scheduling for {1} seconds"),
                            new Object[]{instanceId, sinceStarted / 1000});
                if (sinceStarted > schedulingTimeoutMillis) {
                    LOGGER.log(Level.WARNING, AnkaLog.prefix("Instance {0} reached it's scheduling timeout of {1} seconds, terminating provisioning"),
                            new Object[]{instanceId, schedulingTimeout});
                    cloud.terminateVMInstance(instanceId);
                    return null;
                }
                Thread.sleep(vmCheckTime);
                continue;
            }

            if (instance.isTerminatingOrTerminated() || instance.isInError()) {
                LOGGER.log(Level.WARNING, AnkaLog.prefix("Instance {0} is in unexpected state {1}"),
                        new Object[]{instanceId, instance.getSessionState()});
                cloud.terminateVMInstance(instanceId);
                return null;
            }
        }
    }
}
