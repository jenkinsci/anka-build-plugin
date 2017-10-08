package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.TaskListener;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.ComputerListener;
import hudson.slaves.NodeProperty;
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
                                List<? extends NodeProperty<?>> nodeProperties,
                                AnkaCloudSlaveTemplate template, AnkaMgmtVm vm) throws Descriptor.FormException, IOException {
        super(name, nodeDescription, remoteFS, numExecutors, mode, labelString,
                launcher, template.getRetentionStrategy(), nodeProperties, template, vm);
    }


    public static String generateName(AnkaCloudSlaveTemplate template){
        String randomString = RandomStringUtils.randomAlphanumeric(16);
        return template.getCapsuleNamePrefix() + template.getMasterVmId() + randomString;
    }

    public static AnkaOnDemandSlave createProvisionedSlave(AnkaCloudSlaveTemplate template, Label label, AnkaMgmtVm vm, String name)
            throws IOException, AnkaMgmtException, Descriptor.FormException, InterruptedException {

        AnkaMgmtCloud.Log("vm %s is booting...", vm.getId());
        vm.waitForBoot();
        AnkaMgmtCloud.Log("vm %s %s is booted, creating ssh launcher", vm.getId(), vm.getName());
        SSHLauncher launcher = new SSHLauncher(vm.getConnectionIp(), vm.getConnectionPort(),
                template.getCredentialsId(),
                null, null, null, null, launchTimeoutSeconds, maxNumRetries, retryWaitTime);

        AnkaCloudLauncher delegateLauncher = new AnkaLauncher(vm, launcher);

        AnkaMgmtCloud.Log("launcher created for vm %s %s", vm.getId(), vm.getName());
        return new AnkaOnDemandSlave(name, template.getTemplateDescription(), template.getRemoteFS(),
                template.getNumberOfExecutors(),
                template.getMode(),
                label.toString(),
                /*delegateLauncher*/launcher,
                new ArrayList<NodeProperty<?>>(), template, vm);
    }


    public void setDescription(String jobAndNumber) {
        this.description = String.format("prefix: %s, master image: %s, job name and build number: %s, vm info: (%s)",
                template.getCapsuleNamePrefix(), template.getMasterVmId(), jobAndNumber, this.vm.getInfo());

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
