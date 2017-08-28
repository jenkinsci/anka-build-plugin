package com.veertuci.plugins.anka;

import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertuci.plugins.AnkaMgmtCloud;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.TaskListener;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.ComputerListener;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;
import org.apache.commons.lang.RandomStringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by asafgur on 16/11/2016.
 */
public class AnkaOnDemandSlave extends AbstractAnkaSlave {

    private boolean hadProblemsInBuild = false;
    private String description;
    private boolean acceptingTasks = true;

    protected AnkaOnDemandSlave(String name, String nodeDescription, String remoteFS, int numExecutors,
                                Mode mode, String labelString, ComputerLauncher launcher,
                                RetentionStrategy retentionStrategy, List<? extends NodeProperty<?>> nodeProperties,
                                AnkaCloudSlaveTemplate template, AnkaMgmtVm vm) throws Descriptor.FormException, IOException {
        super(name, nodeDescription, remoteFS, numExecutors, mode, labelString,
                launcher, retentionStrategy, nodeProperties, template, vm);
    }


    public static String generateName(AnkaCloudSlaveTemplate template){
        String randomString = RandomStringUtils.randomAlphanumeric(16);
        return template.getCapsuleNamePrefix() + template.getMasterVmId() + randomString;
    }

    public static AnkaOnDemandSlave createProvisionedSlave(AnkaCloudSlaveTemplate template, Label label,
                                                           AnkaMgmtVm vm, String name) throws IOException, Descriptor.FormException, InterruptedException {

        try {
            vm.waitForBoot();
        } catch (AnkaMgmtException e) {
            e.printStackTrace();
            throw new IOException(e);

        }
        AnkaMgmtCloud.Log("vm %s %s is ready, creating ssh launcher", vm.getId(), vm.getName());
        ComputerLauncher delegateLauncher = createLauncher(vm, template);
        AnkaMgmtCloud.Log("launcher created for vm %s %s", vm.getId(), vm.getName());
        return new AnkaOnDemandSlave(name, template.getTemplateDescription(), template.getRemoteFS(),
                template.getNumberOfExecutors(),
                template.getMode(),
                label.toString(),
                new AnkaCloudLauncher(delegateLauncher),
                new RunOnceCloudRetentionStrategy(1),
                new ArrayList<NodeProperty<?>>(), template, vm);
    }


    public static ComputerLauncher createLauncher(AnkaMgmtVm vm, AnkaCloudSlaveTemplate template) {
        SSHLauncher launcher = new SSHLauncher(vm.getConnectionIp(), vm.getConnectionPort(),
                template.getCredentialsId(),
                null, null, null, null, launchTimeoutSeconds, maxNumRetries, retryWaitTime);

        return launcher;
    }

    public void setDescription(String jobAndNumber) {
        this.description = String.format("prefix: %s, master image: %s, job name and build number: %s, vm info: (%s)",
                template.getCapsuleNamePrefix(), template.getMasterVmId(), jobAndNumber, this.vm.getInfo());

    }

    public boolean isAcceptingTasks() {
        return this.acceptingTasks;
    }

    public void taskAccepted(){
        this.setAcceptingTasks(false);
    }

    public void setAcceptingTasks(boolean isAccepting){
        this.acceptingTasks = isAccepting;
    }

    public String getNodeDescription(){
        return this.description;
    }


    public boolean isKeepAliveOnError(){
        return this.template.isKeepAliveOnError();
    }

    public boolean canTerminate(){
        if(hadProblemsInBuild) {
            if(isKeepAliveOnError()){
                return false;
            }
        }
        return true;
    }

    public void setHadErrorsOnBuild(boolean value){
        this.hadProblemsInBuild = value;
    }

    @Extension
    public static class VeertuCloudComputerListener extends ComputerListener {

        @Override
        public void preLaunch(Computer c, TaskListener taskListener) throws IOException, InterruptedException {
            super.preLaunch(c, taskListener);
        }
    }
}
