package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaVmInfo;
import com.veertu.ankaMgmtSdk.AnkaVmInstance;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import hudson.model.TaskListener;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.DelegatingComputerLauncher;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.SlaveComputer;

import java.io.IOException;

public class AnkaLauncher extends DelegatingComputerLauncher {

    protected static final int defaultLaunchTimeoutSeconds = 2000;
    protected static final int defaultMaxNumRetries = 5;
    protected static final int defaultRetryWaitTime = 5;
    protected static final int defaultSSHLaunchDelay = 15;  // 15 seconds for ssh delay
    protected static final int defaultVmIPAssignWaitSeconds = 10;  // 15 seconds for ssh delay
    protected static final int defaultVmIPAssignRetries = 6;  // 15 seconds for ssh delay
    private final AnkaMgmtCloud cloud;
    private final AnkaCloudSlaveTemplate template;
    private final String instanceId;
    private int launchTimeoutSeconds = 2000;
    private int maxRetries = 5;
    private int retryWaitTime = 5;
    private int sshLaunchDelaySeconds = 15;
    private int vmIPAssignWaitSeconds = 10;
    private int vmIPAssignRetries = 6;

    public AnkaLauncher(AnkaMgmtCloud cloud, AnkaCloudSlaveTemplate template, String instanceId, int launchTimeoutSeconds, int maxRetries, int retryWaitTime, int sshLaunchDelaySeconds, int vmIPAssignWaitSeconds, int vmIPAssignRetries) {
        super(null);
        this.cloud = cloud;
        this.template = template;
        this.instanceId = instanceId;
        if (launchTimeoutSeconds > 0) {
            this.launchTimeoutSeconds = launchTimeoutSeconds;
        }
        if (maxRetries > 0) {
            this.maxRetries = maxRetries;
        }
        if (retryWaitTime > 0) {
            this.retryWaitTime = retryWaitTime;
        }
        if (sshLaunchDelaySeconds > 0) {
            this.sshLaunchDelaySeconds = sshLaunchDelaySeconds;
        }
        if (vmIPAssignWaitSeconds > 0) {
            this.vmIPAssignWaitSeconds = vmIPAssignWaitSeconds;
        }
        if (vmIPAssignRetries > 0) {
            this.vmIPAssignRetries = vmIPAssignRetries;
        }

        ComputerLauncher launcher;
        if (template.getLaunchMethod().equalsIgnoreCase(LaunchMethod.JNLP)) {
            launcher = new JNLPLauncher(template.getJnlpTunnel(), template.getExtraArgs());
        } else if (template.getLaunchMethod().equalsIgnoreCase(LaunchMethod.SSH)) {
            // place holder for ssh, todo: create class
            launcher = new SSHLauncher("", 0, template.getCredentialsId(), template.getJavaArgs(), null, null, null, launchTimeoutSeconds, maxRetries, retryWaitTime, null);
        } else {
            throw new RuntimeException("Unknown launch method");
        }
        this.launcher = launcher;

    }

    public AnkaLauncher(AnkaMgmtCloud cloud, AnkaCloudSlaveTemplate template, String instanceId) {
        this(cloud, template, instanceId, defaultLaunchTimeoutSeconds, defaultMaxNumRetries, defaultRetryWaitTime, defaultSSHLaunchDelay, defaultVmIPAssignWaitSeconds, defaultVmIPAssignRetries);
    }


    @Override
    public boolean isLaunchSupported() {
        return launcher.isLaunchSupported();
    }

    @Override
    public void launch(SlaveComputer computer, TaskListener listener) throws IOException, InterruptedException {
        if (!(computer instanceof AnkaCloudComputer)) {
            throw new RuntimeException("This is not a an Anka Computer");
        }
        AnkaCloudComputer ankaCloudComputer = (AnkaCloudComputer) computer;
        try {

            ankaCloudComputer.reportLaunching();
            AnkaVmInstance instance = cloud.showInstance(instanceId);
            if (instance == null) {
                return;
            }
            listener.getLogger().printf("Instance %s is %s%n", instanceId, instance.getSessionState());
            if (instance.isStarted()) {
                listener.getLogger().printf("Instance %s is Started%n", instanceId);
                AnkaVmInfo vmInfo = instance.getVmInfo();
                if (vmInfo != null) {
                    if (template.getLaunchMethod().equalsIgnoreCase(LaunchMethod.SSH)) {
                        Thread.sleep(sshLaunchDelaySeconds * 1000L);

                        String ip;
                        int port = vmInfo.getForwardedPort(template.SSHPort);

                        if (port != 0) {
                            listener.getLogger().println("SSH port forwarding detected, host's ip will be used");
                            ip = vmInfo.getHostIp();

                        } else {
                            listener.getLogger().println("No SSH port forwarding detected, assuming bridged interface, VM's ip will be used");
                            ip = vmInfo.getVmIp();
                            port = template.SSHPort;

                            int retries = 0;
                            while ((ip == null || ip.isEmpty()) && retries++ < vmIPAssignRetries) {
                                listener.getLogger().printf("Waiting for VM's IP to be assigned, retying attempt %d...%n", retries);

                                Thread.sleep(vmIPAssignWaitSeconds * 1000L);
                                instance = cloud.showInstance(instanceId);
                                if (instance == null) {
                                    listener.getLogger().printf("Instance %s no longer exists%n", instanceId);
                                    return;
                                }

                                vmInfo = instance.getVmInfo();
                                if (vmInfo == null) {
                                    listener.getLogger().printf("Failed to get VM info for %s%n", instanceId);
                                    continue;
                                }

                                ip = vmInfo.getVmIp();
                            }
                        }

                        if (ip == null || ip.isEmpty()) {
                            listener.getLogger().println("Failed to get IP for SSH connection");
                            return;
                        }

                        if (port == 0) {
                            listener.getLogger().println("Failed to get port for SSH connection");
                            return;
                        }

                        this.launcher = new SSHLauncher(ip, port, template.getCredentialsId(), template.getJavaArgs(), null, null, null, launchTimeoutSeconds, maxRetries, retryWaitTime, null);

                        listener.getLogger().printf("Launching SSH connection to %s:%d for instance %s%n", ip, port, instanceId);
                        this.launcher.launch(computer, listener);
                        ankaCloudComputer.reportLaunchFinished();

                    } else if (template.getLaunchMethod().equalsIgnoreCase(LaunchMethod.JNLP)) {
                        listener.getLogger().printf("Launching JNLP for %s%n", instanceId);
                        int numRetries = 0;
                        while (true) {
                            try {
                                if (numRetries > maxRetries) {
                                    break;
                                }
                                this.launcher.launch(computer, listener);
                                if (computer.isOnline()) {
                                    ankaCloudComputer.reportLaunchFinished();
                                    break;
                                }
                            } catch (IOException | InterruptedException e) {
                                if (numRetries >= maxRetries) {
                                    throw e;
                                }
                            } finally {
                                numRetries++;
                                Thread.sleep(retryWaitTime * 1000L);
                            }
                        }

                    } else {
                        listener.getLogger().printf("Unknown launcher for %s%n", instanceId);
                        return;
                    }
                    AbstractAnkaSlave node = (AbstractAnkaSlave) computer.getNode();
                    if (node != null) {
                        node.setDisplayName(vmInfo.getName());
                    }

                }
            } else {
                listener.getLogger().printf("Instance %s is in state %s%n", instanceId, instance.getSessionState());
            }
        } catch (AnkaMgmtException e) {
            throw new IOException(e);
        } finally {
            ankaCloudComputer.reportLaunchFinished();
        }

    }
}
