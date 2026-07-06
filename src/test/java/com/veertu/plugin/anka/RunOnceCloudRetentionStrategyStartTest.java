package com.veertu.plugin.anka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Reproduction test for the orphaned-VM regression: a run-once VM that connects but never runs a
 * task is never reaped.
 *
 * <p>{@link RunOnceCloudRetentionStrategy#check} short-circuits at its {@code !afterFirstConnection()}
 * guard, so the idle timeout is only reachable once {@link AnkaCloudComputer#firstConnectionAttempted()}
 * has been called. On the normal (Jenkins-already-running) provisioning path, that flag was only ever
 * set inside {@code AnkaPlannedNodeCreator.waitAndConnect} via {@code slave.toComputer()} - but the
 * node's computer is frequently not registered yet when that runs (lightweight mode never adds it
 * early), so {@code toComputer()} returns {@code null} and the flag stays {@code false} forever.
 * {@link RunOnceCloudRetentionStrategy#start} connected the computer but did not set the flag, leaving
 * the VM orphaned.
 *
 * <p>This test FAILS against the pre-fix code (start() calls connect but never marks the first
 * connection) and PASSES once start() marks it on the normal path too.
 */
@WithJenkins
public class RunOnceCloudRetentionStrategyStartTest {

    @SuppressWarnings("unused")
    private JenkinsRule jenkinsRule;

    @BeforeEach
    void setUp(JenkinsRule jenkinsRule) {
        this.jenkinsRule = jenkinsRule;
    }

    @Test
    public void startMustMarkFirstConnectionOnNormalPath() {
        AnkaCloudComputer computer = mock(AnkaCloudComputer.class);
        when(computer.getName()).thenReturn("orphan-repro");

        new RunOnceCloudRetentionStrategy(1).start(computer);

        // The connection must be initiated AND the first-connection flag set, in that order, so the
        // retention strategy's idle-timeout path is reachable and the VM can be reaped.
        InOrder order = inOrder(computer);
        order.verify(computer).connect(false);
        order.verify(computer).firstConnectionAttempted();
    }
}
