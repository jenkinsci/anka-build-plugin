package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaVmInstance;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.model.Descriptor;
import hudson.model.Executor;
import hudson.model.ExecutorListener;
import hudson.model.Queue;
import hudson.slaves.RetentionStrategy;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by avia on 12/07/2016.
 */
public class RunOnceCloudRetentionStrategy extends RetentionStrategy<AnkaCloudComputer> implements ExecutorListener, Cloneable {
    private static final transient Logger LOGGER = Logger.getLogger(RunOnceCloudRetentionStrategy.class.getName());

    private int idleMinutes;
    private int reconnectionRetries = 0;
    private static final int MAX_RECONNECTION_RETRIES = 7;


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
                LOGGER.log(Level.FINE, "Computer {0} has {1} busy executors", new Object[]{computer.getName(), computer.countBusy()});
                return idleMinutes;
            }

            if (computer.isSchedulingOrPulling()) { // it's scheduling or pulling - wait
                return idleMinutes * 3;
            }
            AbstractAnkaSlave node = computer.getNode();
            if (node != null && !node.isAlive()) {
                done(computer);
            }

            if (computer.isConnecting() || !computer.afterFirstConnection()) {
                return idleMinutes;
            }


            if (reconnectionRetries >= MAX_RECONNECTION_RETRIES) { // we tried to reconnect - but it's enough now
                LOGGER.log(Level.WARNING, "Computer {0}, instance {1} is terminating because it has reached it's max reconnection retries",
                        new Object[]{computer.getName(), computer.getVMId()});
                done(computer);
                return idleMinutes;
            }

            if (!computer.isOnline()) {
                LOGGER.log(Level.WARNING, "Computer {0}, instance {1} is offline, trying to reconnect",
                        new Object[]{computer.getName(), computer.getVMId()});
                boolean forceReconnect = false;
                if (reconnectionRetries > 4) {
                    forceReconnect = true;  // only force reconnect on first retry, after no answer for a while
                }
                computer.connect(forceReconnect);
                reconnectionRetries++;
                return idleMinutes;
            }


            if(computer.isIdle()) {
                final long idleMilliseconds = System.currentTimeMillis() - computer.getIdleStartMilliseconds();
                if(idleMilliseconds > TimeUnit.MINUTES.toMillis(idleMinutes)) {
                    LOGGER.log(Level.WARNING, "Computer {0}, instance {1} is terminating due to idle timeout",
                            new Object[]{computer.getName(), computer.getVMId()});
                    done(computer);
                }
            }
            return idleMinutes;
        } catch (ClassCastException e){
            return idleMinutes;  // these are not the droids you are looking for
        }
    }


    @Override
    public void taskAccepted(Executor executor, Queue.Task task) {
        AnkaCloudComputer computer = (AnkaCloudComputer) executor.getOwner();
        LOGGER.log(Level.INFO, "Computer {0}, instance {2} accepted task {1}",
                new Object[]{computer.getName(), task.toString(), computer.getVMId()});
    }

    @Override
    public void taskCompleted(final Executor executor, final Queue.Task task, final long durationMS) {
        AnkaCloudComputer computer = (AnkaCloudComputer) executor.getOwner();
        LOGGER.log(Level.INFO, "Computer {0}, instance {2} completed task {1}",
                new Object[]{computer.getName(), task.toString(), computer.getVMId()});
        computer.setAcceptingTasks(false);
        done(computer);
    }

    @Override
    public void taskCompletedWithProblems(final Executor executor, final Queue.Task task, final long durationMS, final Throwable problems) {
        AnkaCloudComputer computer = (AnkaCloudComputer) executor.getOwner();
        LOGGER.log(Level.INFO, "Computer {0}, instance {2} accepted task {1} with problems",
                new Object[]{computer.getName(), task.toString(), computer.getVMId()});
        computer.setAcceptingTasks(false);
        done(computer);
    }

    private void done(final AnkaCloudComputer computer) {
        AnkaMgmtCloud.Log("Computer %s is done, terminating", computer.getName());
        AbstractAnkaSlave node = computer.getNode();
        if (node != null) {
            AnkaMgmtCloud.Log("computer %s node %s found", computer.getName(), node.getNodeName());
            if (computer.countBusy() > 1) {
                AnkaMgmtCloud.Log("computer %s is busy, not terminating", computer.getName());
                return;
            }
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
    public void start(final @Nonnull AnkaCloudComputer computer) {
        //Jenkins is in the process of starting up
        if (Jenkins.get().getInitLevel() != InitMilestone.COMPLETED) {
            String vmId = computer.getVMId();
            String cloudName = computer.getCloudName();
            AnkaMgmtCloud cloud = (AnkaMgmtCloud) Jenkins.get().getCloud(cloudName);
            if (cloud != null) {
                try {
                    AnkaVmInstance instance = cloud.showInstance(vmId);
                    if (instance == null || instance.isTerminatingOrTerminated() || instance.isInError()) {
                        AbstractAnkaSlave node = computer.getNode();
                        if (node != null) {
                            node.terminate();
                        } else {
                            cloud.terminateVMInstance(vmId); // if there is no node terminate the instance
                        }
                        return;
                    }
                    // means we have an instance
                    computer.connect(true); // try to force reconnection
                    computer.firstConnectionAttempted(); // in case jenkins restarted before we could set this property
                    return;
                } catch (AnkaMgmtException | IOException e) {
                    LOGGER.info("Got exception while handling node in jenkins startup");
                    e.printStackTrace();
                    return;
                }
            }
        }


        LOGGER.info("Start requested for " + computer.getName());
        computer.connect(false);
    }

    public RunOnceCloudRetentionStrategy clone() throws CloneNotSupportedException {
        return (RunOnceCloudRetentionStrategy) super.clone();
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
