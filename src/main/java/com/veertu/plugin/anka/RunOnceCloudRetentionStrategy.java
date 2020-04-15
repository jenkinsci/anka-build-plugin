package com.veertu.plugin.anka;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Executor;
import hudson.model.ExecutorListener;
import hudson.model.Queue;
import hudson.slaves.RetentionStrategy;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by avia on 12/07/2016.
 */
public class RunOnceCloudRetentionStrategy extends RetentionStrategy<AnkaCloudComputer> implements ExecutorListener {
    private static final transient Logger LOGGER = Logger.getLogger(RunOnceCloudRetentionStrategy.class.getName());

    private int idleMinutes = 1;
    private int reconnectionRetries = 0;
    private static final int MAX_RECONNECTION_RETRIES = 5;


    @DataBoundConstructor
    public RunOnceCloudRetentionStrategy(int idleMinutes) {
        this.idleMinutes = idleMinutes;
    }

    public int getIdleMinutes() {
        return idleMinutes;
    }

    @Override
    public long check(final AnkaCloudComputer computer) {
        try {
            LOGGER.log(Level.INFO, "Checking computer {0}", computer.getName());
            if (computer.countBusy() > 1) {
                LOGGER.log(Level.INFO, "Computer {0} has {1} busy executors", new Object[]{computer.getName(), computer.countBusy()});
                return 1;
            }

            if (computer.isAcceptingTasks()) {  // this computer is still waiting to run a job or running a job
                LOGGER.log(Level.INFO, "Computer {0} is waiting to accept tasks", computer.getName());
                return 1;
            }


            if (reconnectionRetries >= MAX_RECONNECTION_RETRIES) { // we tried to reconnect - but it's enough now
                LOGGER.log(Level.INFO, "Computer {0} has reached it's max reconnection retries", computer.getName());
                done(computer);
                return 1;
            }

            if (!computer.isOnline()) {
                LOGGER.log(Level.INFO, "Computer {0} is offline, trying to reconnect", computer.getName());
                boolean forceReconnect = true;
                if (reconnectionRetries > 0) {  // only force reconnect on first retry, after that don't stop a connecting process
                    forceReconnect = false;
                }
                computer.connect(forceReconnect);
                reconnectionRetries++;
                return 1;
            }


            if(computer.isIdle()) {
                final long idleMilliseconds = System.currentTimeMillis() - computer.getIdleStartMilliseconds();
                if(idleMilliseconds > TimeUnit.MINUTES.toMillis(idleMinutes)) {
                    AnkaMgmtCloud.Log("Disconnecting %s due to idle timeout", computer.getName());
                    done(computer);
                }
            }
            return 1;
        } catch (ClassCastException e){
            return 1;  // these are not the droids you are looking for
        }
    }


    @Override
    public void taskAccepted(Executor executor, Queue.Task task) {
        AnkaCloudComputer computer = (AnkaCloudComputer) executor.getOwner();
        LOGGER.log(Level.INFO, "Computr {0} accepted task {1}", new Object[]{computer.getName(), task.toString()});
    }

    @Override
    public void taskCompleted(final Executor executor, final Queue.Task task, final long durationMS) {
        AnkaCloudComputer computer = (AnkaCloudComputer) executor.getOwner();
        computer.setAcceptingTasks(false);
        done(executor);
    }

    @Override
    public void taskCompletedWithProblems(final Executor executor, final Queue.Task task, final long durationMS, final Throwable problems) {
        done(executor);
    }

    private void done(final Executor executor) {
        final AnkaCloudComputer computer = (AnkaCloudComputer) executor.getOwner();
        done(computer);
    }

    private void done(final AnkaCloudComputer computer) {
        AnkaMgmtCloud.Log("Computer %s is done, terminating", computer.getName());
        AbstractAnkaSlave node = computer.getNode();
        if (node != null) {
            AnkaMgmtCloud.Log("computer %s node %s found", computer.getName(), node.getNodeName());
            if ( node.canTerminate()) {
                AnkaMgmtCloud.Log("terminating computer %s node %s", computer.getName(), node.getNodeName());
                try {
                    node.terminate();
                } catch (IOException e) {
                    AnkaMgmtCloud.Log("Failed to terminate " + computer.getName(), e);
                }
            }
            else {
                AnkaMgmtCloud.Log("not terminating computer %s node %s due to termination configuration",
                        computer.getName(), node.getNodeName());
            }
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    @Restricted(NoExternalUse.class)
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @Extension
    public static final class DescriptorImpl extends Descriptor<RetentionStrategy<?>> {
        @Override
        public String getDisplayName() {
            return "Run Once Cloud Retention Strategy";
        }
    }

}
