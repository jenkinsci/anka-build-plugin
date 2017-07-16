package com.veertuci.plugins;

import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import com.veertuci.plugins.anka.AnkaOnDemandSlave;
import com.veertuci.plugins.anka.AnkaCloudSlaveTemplate;
import com.veertuci.plugins.anka.exceptions.AnkaHostException;
import hudson.model.Computer;
import hudson.model.Label;
import hudson.model.Node;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;

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
                            return tryToCallSlave(slave);
                        }
                        return tryToCallSlave(slave);
                    }

            };
            Future<Node> f = Computer.threadPoolForRemoting.submit(provisionNodeCallable);
            return new AnkaPlannedNode(slave.getDisplayName(), f, numberOfExecutors);

    }

    public static AnkaPlannedNode createInstance(final AnkaCloudSlaveTemplate template,
                                                 final Label label, final AnkaMgmtVm vm) throws AnkaHostException, IOException{
        final int numberOfExecutors = template.getNumberOfExecutors();
        final String name = AnkaOnDemandSlave.generateName(template);
        final Callable<Node> provisionNodeCallable = new Callable<Node>() {
            public Node call() throws Exception {
                AnkaOnDemandSlave slave = AnkaOnDemandSlave.createProvisionedSlave(template, label, vm, name);
                AnkaMgmtCloud.Log("got a slave adding it to jenkins");
                Jenkins.getInstance().addNode(slave);
                long startTime = System.currentTimeMillis(); // fetch starting time
                while ((System.currentTimeMillis() - startTime) < slave.launchTimeout * 1000) {
                    try {
                        AnkaMgmtCloud.Log("trying to init slave %s on vm", slave.getDisplayName());
                        return tryToCallSlave(slave);
                    }
                    catch (ExecutionException e)
                    {
                        AnkaMgmtCloud.Log("caught ExecutionException when trying to start %s " +
                                "sleeping for 5 seconds to retry", slave.getNodeName());
                        Thread.sleep(5000);
                        continue;
                    }
                    catch (NullPointerException e){
                        AnkaMgmtCloud.Log("vm quit unexpectedly for slave %s", slave.getNodeName());
                        slave.terminate();
                        break;
                    }
                }
                return tryToCallSlave(slave);
            }

        };
        Future<Node> f = Computer.threadPoolForRemoting.submit(provisionNodeCallable);
        return new AnkaPlannedNode(name, f, numberOfExecutors);

    }

    private static AnkaOnDemandSlave tryToCallSlave(AnkaOnDemandSlave slave) throws ExecutionException, InterruptedException {
        Computer computer = slave.toComputer();
        Future<?> f = computer.connect(false);
        f.get();
        return slave;
    }
}
