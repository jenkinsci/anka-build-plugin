package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaVmInfo;
import com.veertu.ankaMgmtSdk.AnkaVmInstance;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import hudson.model.TaskListener;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.*;

import java.io.IOException;

public class AnkaLauncher extends DelegatingComputerLauncher {

    private final AnkaMgmtCloud cloud;
    private final AnkaCloudSlaveTemplate template;
    private final String instanceId;
    private int launchTimeoutSeconds = 2000;
    private int maxRetries = 5;
    private int retryWaitTime = 5;
    private int sshLaunchDelaySeconds = 15;


    protected static final int defaultLaunchTimeoutSeconds = 2000;
    protected static final int defaultMaxNumRetries = 5;
    protected static final int defaultRetryWaitTime = 5;
    protected static final int defaultSSHLaunchDelay = 15;  // 15 seconds for ssh delay

    public AnkaLauncher(AnkaMgmtCloud cloud, AnkaCloudSlaveTemplate template, String instanceId ,
                        int launchTimeoutSeconds, int maxRetries, int retryWaitTime, int sshLaunchDelaySeconds){
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

        ComputerLauncher launcher;
        if (template.getLaunchMethod().equalsIgnoreCase(LaunchMethod.JNLP)) {
            launcher = new JNLPLauncher(template.getJnlpTunnel(),
                    template.getExtraArgs());
        } else if (template.getLaunchMethod().equalsIgnoreCase(LaunchMethod.SSH)) {
            // place holder for ssh, todo: create class
            launcher = new SSHLauncher("", 0,
                    template.getCredentialsId(),
                    template.getJavaArgs(), null, null, null,
                    launchTimeoutSeconds, maxRetries, retryWaitTime, null);
        } else {
            throw new RuntimeException("Unknown launch method");
        }
        this.launcher = launcher;

    }
    public AnkaLauncher(AnkaMgmtCloud cloud, AnkaCloudSlaveTemplate template, String instanceId) {
        this(cloud, template, instanceId, defaultLaunchTimeoutSeconds,
                defaultMaxNumRetries, defaultRetryWaitTime, defaultSSHLaunchDelay);
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
            listener.getLogger().println(String.format("Instance %s is %s", instanceId, instance.getSessionState()));
            if (instance.isStarted()) {
                listener.getLogger().println(String.format("Instance %s is Started", instanceId));
                AnkaVmInfo vmInfo = instance.getVmInfo();
                if (vmInfo != null ){
                    if (template.getLaunchMethod().equalsIgnoreCase(LaunchMethod.SSH)) {
                        String hosIp = vmInfo.getHostIp();
                        if (hosIp == null ) {
                            return;
                        }
                        this.launcher = new SSHLauncher(hosIp, vmInfo.getForwardedPort(template.SSHPort),
                                template.getCredentialsId(),
                                template.getJavaArgs(), null, null, null,
                                launchTimeoutSeconds, maxRetries, retryWaitTime, null);
                        Thread.sleep(sshLaunchDelaySeconds * 1000);
                        listener.getLogger().println(String.format("Launching SSH connection for %s", instanceId));
                        this.launcher.launch(computer, listener);
                        ankaCloudComputer.reportLaunchFinished();

                    } else if (template.getLaunchMethod().equalsIgnoreCase(LaunchMethod.JNLP)) {
                        listener.getLogger().println(String.format("Launching JNLP for %s", instanceId));
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
                                Thread.sleep(retryWaitTime * 1000);
                            }
                        }

                    } else {
                        listener.getLogger().println(String.format("Unknown launcher for %s", instanceId));
                        return;
                    }
                    AbstractAnkaSlave node = (AbstractAnkaSlave) computer.getNode();
                    if (node != null) {
                        node.setDisplayName(vmInfo.getName());
                    }

                }
            } else {
                listener.getLogger().println(String.format("Instance %s is in state %s", instanceId, instance.getSessionState()));
            }
        } catch (AnkaMgmtException e) {
            throw new IOException(e);
        } finally {
            ankaCloudComputer.reportLaunchFinished();
        }

    }

    @Override
    public void afterDisconnect(SlaveComputer computer, TaskListener listener) {
        // Deadlock can occur if this is called while launcher is still in progress
        // Here we wait gracefully for the launching to finish
        
        int maxWaitingTime = 180;  // 3 minutes

        if (!(computer instanceof AnkaCloudComputer)) {
            throw new RuntimeException("This is not a an Anka Computer");
        }
        AnkaCloudComputer ankaCloudComputer = (AnkaCloudComputer) computer;
        int timeWaited = 0;

        while (true) {
            if (! ankaCloudComputer.isLaunching())
                break;

            if (timeWaited > maxWaitingTime) {
                listener.getLogger().println("afterDisconnect hook: waiting for launcher to finish timed out");
                break;
            }

            try {
                timeWaited += 1;
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {
                listener.getLogger().println("afterDisconnect hook: waiting for launcher to finish interrupted");
            }
        }
        this.launcher.afterDisconnect(computer, listener);
    }
}
