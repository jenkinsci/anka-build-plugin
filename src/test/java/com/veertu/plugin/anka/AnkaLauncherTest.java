package com.veertu.plugin.anka;

import hudson.model.TaskListener;
import hudson.plugins.sshslaves.SSHLauncher;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;

public class AnkaLauncherTest {

    @Test
    public void isTcpPortOpenReturnsTrueWhenPortIsListening() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            assertThat(AnkaLauncher.isTcpPortOpen("127.0.0.1", serverSocket.getLocalPort(), 2000), is(true));
        }
    }

    @Test
    public void isTcpPortOpenReturnsFalseWhenPortIsClosed() {
        assertThat(AnkaLauncher.isTcpPortOpen("127.0.0.1", 9, 200), is(false));
    }

    @Test
    public void waitForSshPortReadyReturnsAsSoonAsPortOpens() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            TaskListener listener = taskListenerWithBuffer();
            long startedAtMillis = System.currentTimeMillis();

            AnkaLauncher.waitForSshPortReady(
                    "127.0.0.1",
                    serverSocket.getLocalPort(),
                    30,
                    AnkaLauncher.defaultSSHPollIntervalSeconds,
                    listener);

            assertThat(System.currentTimeMillis() - startedAtMillis, is(lessThan(3000L)));
        }
    }

    @Test
    public void sshLauncherReceivesConfiguredJavaPath() {
        AnkaCloudSlaveTemplate template = new AnkaCloudSlaveTemplate();
        template.setLaunchMethod(LaunchMethod.SSH);
        template.setJavaPath("/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java");

        AnkaLauncher ankaLauncher = new AnkaLauncher(null, template, "instance-1");

        assertThat(ankaLauncher.getLauncher(), instanceOf(SSHLauncher.class));
        SSHLauncher sshLauncher = (SSHLauncher) ankaLauncher.getLauncher();
        assertThat(sshLauncher.getJavaPath(), is("/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java"));
    }

    @Test
    public void sshLauncherReceivesBlankJavaPathWhenUnset() {
        AnkaCloudSlaveTemplate template = new AnkaCloudSlaveTemplate();
        template.setLaunchMethod(LaunchMethod.SSH);

        AnkaLauncher ankaLauncher = new AnkaLauncher(null, template, "instance-1");

        assertThat(ankaLauncher.getLauncher(), instanceOf(SSHLauncher.class));
        SSHLauncher sshLauncher = (SSHLauncher) ankaLauncher.getLauncher();
        // Template javaPath is null; SSHLauncher normalizes null to "".
        assertThat(template.getJavaPath(), is(nullValue()));
        assertThat(sshLauncher.getJavaPath(), is(""));
    }

    private static TaskListener taskListenerWithBuffer() {
        return new TaskListener() {
            private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            @Override
            public PrintStream getLogger() {
                return new PrintStream(buffer, true);
            }
        };
    }
}
