package com.veertu.plugin.anka;
import hudson.model.*;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.CloudRetentionStrategy;
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
public class RunOnceCloudRetentionStrategy extends CloudRetentionStrategy implements ExecutorListener {

    public static final Logger logger = Logger.getLogger(RunOnceCloudRetentionStrategy.class.getName());

    private int idleMinutes = 1;
    private transient boolean terminating;

    @DataBoundConstructor
    public RunOnceCloudRetentionStrategy(int idleMinutes) {
        super(idleMinutes);
        this.idleMinutes = idleMinutes;
    }

    public int getIdleMinutes() {
        return idleMinutes;
    }

    @Override
    public long check(final AbstractCloudComputer c) {
        if(c.isIdle() && !disabled) {
            final long idleMilliseconds = System.currentTimeMillis() - c.getIdleStartMilliseconds();
            if(idleMilliseconds > TimeUnit.MINUTES.toMillis(idleMinutes)) {
                logger.log(Level.FINE, "Disconnecting {0}", c.getName());
                done(c);
            }
        }
        return 1;
    }


    @Override
    public void taskAccepted(Executor executor, Queue.Task task) {

    }

    @Override
    public void taskCompleted(final Executor executor, final Queue.Task task, final long durationMS) {
        done(executor);
    }

    @Override
    public void taskCompletedWithProblems(final Executor executor, final Queue.Task task, final long durationMS, final Throwable problems) {
        done(executor);
    }

    private void done(final Executor executor) {
        final AbstractCloudComputer<?> c = (AbstractCloudComputer) executor.getOwner();
        final Queue.Executable exec = executor.getCurrentExecutable();
        logger.log(Level.FINE, "terminating {0} since {1} seems to be finished", new Object[]{c.getName(),exec});
        done(c);
    }

    private void done(final AbstractCloudComputer<?> c) {
        AnkaMgmtCloud.Log("computer %s is done - starting retention method", c.getName());
        c.setAcceptingTasks(false);
        Computer.threadPoolForRemoting.submit(new Runnable() {
            @Override
            public void run() {
                Queue.withLock(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            AnkaMgmtCloud.Log("terminating %s (in RetentionStrategy)", c.getName());
                            AbstractCloudSlave node = c.getNode();

                            if(node != null) {
                                AnkaMgmtCloud.Log("computer %s node %s found", c.getName(), node.getNodeName());
                                AnkaOnDemandSlave slave = (AnkaOnDemandSlave) node;
                                if ( slave.canTerminate()) {
                                    AnkaMgmtCloud.Log("terminating computer %s node %s", c.getName(), node.getNodeName());
                                    slave.terminate();
                                }
                                else {
                                    AnkaMgmtCloud.Log("not terminating computer %s node %s due to termination configuration",
                                            c.getName(), node.getNodeName());
                                }
                            }
                            else {
                                AnkaMgmtCloud.Log("node not found for %s", c.getName());

                            }
                        } catch(InterruptedException | IOException e) {
                            logger.log(Level.WARNING, "Failed to terminate " + c.getName(), e);
                            synchronized(RunOnceCloudRetentionStrategy.this) {
                                terminating = false;
                            }
                        }
                    }
                });
            }
        });
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    @Restricted(NoExternalUse.class)
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends Descriptor<RetentionStrategy<?>> {
        @Override
        public String getDisplayName() {
            return "Run Once Cloud Retention Strategy";
        }
    }
}
