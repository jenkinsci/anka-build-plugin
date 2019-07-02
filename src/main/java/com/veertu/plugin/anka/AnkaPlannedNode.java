package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaAPI;
import com.veertu.plugin.anka.exceptions.AnkaHostException;
import hudson.model.Computer;
import hudson.model.Label;
import hudson.model.Node;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import jenkins.model.Jenkins;

/**
 * Created by asafgur on 16/11/2016.
 */
public class AnkaPlannedNode extends NodeProvisioner.PlannedNode{
    /**
     * Construct a PlannedNode instance without {@link Cloud} callback for finalization.
     *
     * @param displayName  Used to display this object in the UI.
     * @param future       Used to launch a @{link Node} object.
     * @param numExecutors The number of executors that will be provided by the launched {@link Node}.
     */
    public AnkaPlannedNode(String displayName, Future<Node> future, int numExecutors) {
        super(displayName, future, numExecutors);
    }

    public static AnkaPlannedNode createInstance(AnkaCloudSlaveTemplate template, final AnkaOnDemandSlave slave){
            final int numberOfExecutors = template.getNumberOfExecutors();
            final Callable<Node> provisionNodeCallable = new Callable<Node>() {
                public Node call() throws Exception {
                        long startTime = System.currentTimeMillis(); // fetch starting time
                        while ((System.currentTimeMillis() - startTime) < slave.launchTimeout * 1000) {
                            tryToCallSlave(slave);
                            break;

                        }
                        return slave;
                    }

            };
            Future<Node> f = Computer.threadPoolForRemoting.submit(provisionNodeCallable);
            return new AnkaPlannedNode(slave.getDisplayName(), f, numberOfExecutors);
    }

    public static AnkaPlannedNode createInstance(final AnkaAPI ankaAPI, final AnkaCloudSlaveTemplate template,
                                                 final Label label) throws AnkaHostException, IOException{
        final int numberOfExecutors = template.getNumberOfExecutors();
        final String name = AnkaOnDemandSlave.generateName(template);
        final Callable<Node> provisionNodeCallable = new Callable<Node>() {
            public Node call() throws Exception {
                AnkaOnDemandSlave slave = null;
                try {
                    slave = AnkaOnDemandSlave.createProvisionedSlave(ankaAPI, template, label);
                }
                catch  (Exception e) {
                    AnkaMgmtCloud.Log("createProvisionedSlave() caught exception %s", e.getMessage());
                    //slave.terminate();
                    e.printStackTrace();
                    throw e;
                }
                if (template.getLaunchMethod() == LaunchMethod.SSH) {
                    return slave;
                }
                AnkaMgmtCloud.Log("got a slave adding it to jenkins");
                try {
                    Jenkins.getInstance().addNode(slave);
                }
                catch  (Exception e) {
                    AnkaMgmtCloud.Log("Failed to add slave %s", slave.getDisplayName());
                    slave.terminate();
                    throw e;
                }
                long startTime = System.currentTimeMillis(); // fetch starting time
                while (true) {
                    try {
                        AnkaMgmtCloud.Log("trying to init slave %s on vm", slave.getDisplayName());
                        tryToCallSlave(slave);
                        break;
                    }
                    catch (ExecutionException e) {
                        if ((System.currentTimeMillis() - startTime) < slave.launchTimeout * 1000){
                            AnkaMgmtCloud.Log("caught ExecutionException when trying to start %s " +
                                    "sleeping for 5 seconds to retry", slave.getNodeName());
                            Thread.sleep(1000);
                            continue;
                        }
                        AnkaMgmtCloud.Log("Failed to connect to slave %s", slave.getNodeName());
                        slave.terminate();
                        throw e;
                    }
                    catch (Exception e) {
                        AnkaMgmtCloud.Log("vm quit unexpectedly for slave %s", slave.getNodeName());
                        slave.terminate();
                        throw e;
                    }
                }
                return slave;
            }

        };
        Future<Node> f = Computer.threadPoolForRemoting.submit(provisionNodeCallable);
        return new AnkaPlannedNode(name, f, numberOfExecutors);

    }

    private static void tryToCallSlave(AnkaOnDemandSlave slave) throws ExecutionException, InterruptedException {
        Computer computer = slave.toComputer();
        if (computer == null) {
            throw new ExecutionException("No link to computer", new NullPointerException("computer link is null"));
        }
        Future<?> f = computer.connect(false);
        f.get();
    }
}
