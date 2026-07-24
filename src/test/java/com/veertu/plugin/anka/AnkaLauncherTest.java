package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaVmInfo;
import com.veertu.ankaMgmtSdk.AnkaVmInstance;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    public void launchRecreatesSshLauncherWithConfiguredJavaPath() throws Exception {
        String javaPath = "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java";
        AnkaCloudSlaveTemplate template = new AnkaCloudSlaveTemplate();
        template.setLaunchMethod(LaunchMethod.SSH);
        template.setJavaPath(javaPath);
        template.setSSHPort(22);

        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int forwardedPort = serverSocket.getLocalPort();

            AnkaVmInfo vmInfo = mock(AnkaVmInfo.class);
            when(vmInfo.getForwardedPort(anyInt())).thenReturn(forwardedPort);
            when(vmInfo.getHostIp()).thenReturn("127.0.0.1");

            AnkaVmInstance instance = mock(AnkaVmInstance.class);
            when(instance.isStarted()).thenReturn(true);
            when(instance.getSessionState()).thenReturn("Started");
            when(instance.getVmInfo()).thenReturn(vmInfo);

            AnkaMgmtCloud cloud = mock(AnkaMgmtCloud.class);
            when(cloud.showInstance("instance-1")).thenReturn(instance);

            AnkaCloudComputer computer = mock(AnkaCloudComputer.class);
            doNothing().when(computer).reportLaunching();
            doNothing().when(computer).reportLaunchFinished();

            AnkaLauncher ankaLauncher = new AnkaLauncher(
                    cloud, template, "instance-1", 1, 1, 1, 2, 1, 1);
            TaskListener listener = taskListenerWithBuffer();

            try {
                ankaLauncher.launch(computer, listener);
            } catch (Exception ignored) {
                // SSH auth/connect may fail; coverage target is launcher recreation with javaPath.
            }

            assertThat(ankaLauncher.getLauncher(), instanceOf(SSHLauncher.class));
            SSHLauncher sshLauncher = (SSHLauncher) ankaLauncher.getLauncher();
            assertThat(sshLauncher.getJavaPath(), is(javaPath));
            assertThat(sshLauncher.getPort(), is(forwardedPort));
            assertThat(sshLauncher.getHost(), is("127.0.0.1"));
        }
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
