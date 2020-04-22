package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaVmInfo;
import com.veertu.ankaMgmtSdk.AnkaVmInstance;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.slaves.NodeProvisioner;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by asafgur on 16/11/2016.
 */
public class AnkaPlannedNodeCreator {
    private static final transient Logger LOGGER = Logger.getLogger(AnkaPlannedNodeCreator.class.getName());


    public static NodeProvisioner.PlannedNode createPlannedNode(final AnkaMgmtCloud cloud, final AnkaCloudSlaveTemplate template, final AbstractAnkaSlave slave) {
        return new NodeProvisioner.PlannedNode(template.getDisplayName(),
                Computer.threadPoolForRemoting.submit(new Callable<Node>() {


                    public Node call() throws Exception {
                        return waitAndConnect(cloud, template, slave);
                    }
                })
                , template.getNumberOfExecutors());
    }

    public static Node waitAndConnect(final AnkaMgmtCloud cloud, final AnkaCloudSlaveTemplate template, final AbstractAnkaSlave slave) throws AnkaMgmtException, InterruptedException {
        float timeStarted = System.currentTimeMillis();
        while (true) {
            String instanceId = slave.getInstanceId();

            AnkaVmInstance instance = cloud.showInstance(instanceId);
            if (instance == null) {
                LOGGER.log(Level.WARNING, "instance `{0}` not found in cloud {1}. Terminate provisioning ",
                        new Object[]{instanceId, cloud.getCloudName()});
                return null;
            }

            if (instance.isStarted()) {
                AnkaVmInfo vmInfo = instance.getVmInfo();
                if (vmInfo == null) { // shouldn't happen if vm is Started
                    Thread.sleep(5000);
                    continue;
                }
                String hostIp = vmInfo.getHostIp();
                if (hostIp == null || hostIp.isEmpty()) { // the node doesn't have an ip yet
                    Thread.sleep(2000);
                    continue;
                }

                Computer computer = slave.toComputer();
                if ( computer != null ) {
                    computer.connect(false);
                }
                return slave;

            }

            if (instance.isSchedulingOrPulling()) {
                int secondsScheduling = (int)((System.currentTimeMillis() - timeStarted) * 1000);
                if (secondsScheduling > template.getSchedulingTimeout()) {
                    LOGGER.log(Level.WARNING,"Instance {0} reached it's scheduling timeout of {1} seconds, terminating provisioning",
                            new Object[]{instanceId, template.getSchedulingTimeout()});
                    cloud.terminateVMInstance(instanceId);
                    return null;
                }
                Thread.sleep(5000);

            }

            if (instance.isTerminatingOrTerminated() || instance.isInError()) {
                LOGGER.log(Level.WARNING,"Instance {0} is in unexpected state {1}",
                        new Object[]{instanceId, instance.getSessionState()});
                cloud.terminateVMInstance(instanceId);
                return null;
            }

        }
    }

//
//    public static NodeProvisioner.PlannedNode createInstance(final AnkaMgmtCloud cloud, final AnkaCloudSlaveTemplate template) throws AnkaHostException, IOException{
//        final int numberOfExecutors = template.getNumberOfExecutors();
//        final String name = AnkaOnDemandSlave.generateName(template);
//        final Callable<Node> provisionNodeCallable = () -> {
//
//            AnkaOnDemandSlave slave = null;
//            try {
//                slave = AnkaOnDemandSlave.createProvisionedSlave(cloud, template);
//            }
//            catch  (Exception e) {
//                AnkaMgmtCloud.Log("createProvisionedSlave() caught exception %s", e.getMessage());
//                e.printStackTrace();
//                throw e;
//            }
//            if (slave == null) {
//                return  null;
//            }
//            if (template.getLaunchMethod().toLowerCase().equals(LaunchMethod.SSH)) {
//                tryToCallSlave(slave);
//                return slave;
//            }
//            long startTime = System.currentTimeMillis(); // fetch starting time
//            while (true) {
//                try {
//                    AnkaMgmtCloud.Log("trying to init slave %s on vm", slave.getDisplayName());
//                    tryToCallSlave(slave);
//                    break;
//                }
//                catch (ExecutionException e) {
//                    if ((System.currentTimeMillis() - startTime) < slave.launchTimeout * 1000){
//                        AnkaMgmtCloud.Log("caught ExecutionException when trying to start %s " +
//                                "sleeping for 1 seconds to retry", slave.getNodeName());
//                        Thread.sleep(1000);
//                        continue;
//                    }
//                    AnkaMgmtCloud.Log("Failed to connect to slave %s", slave.getNodeName());
//                    slave.terminate();
//                    throw e;
//                }
//                catch (Exception e) {
//                    AnkaMgmtCloud.Log("vm quit unexpectedly for slave %s", slave.getNodeName());
//                    slave.terminate();
//                    throw e;
//                }
//            }
//            return slave;
//        };
//        Future<Node> f = Computer.threadPoolForRemoting.submit(provisionNodeCallable);
//        return new NodeProvisioner.PlannedNode(name, f, numberOfExecutors);
//
//    }
//
//    public static void tryToCallSlave(AnkaOnDemandSlave slave) throws ExecutionException, InterruptedException {
//        Computer computer = slave.toComputer();
//        if (computer == null) {
//            throw new ExecutionException("No link to computer", new NullPointerException("computer link is null"));
//        }
//        Future<?> f = computer.connect(false);
//        f.get();
//    }
}
