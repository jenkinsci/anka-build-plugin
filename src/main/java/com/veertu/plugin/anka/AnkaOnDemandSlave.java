package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import com.veertu.ankaMgmtSdk.AnkaVmFactory;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.TaskListener;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.*;
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
        return template.getMasterVmId() + randomString;
    }

    public static AnkaOnDemandSlave createProvisionedSlave(AnkaCloudSlaveTemplate template, Label label, String mgmtUrl)
            throws IOException, AnkaMgmtException, Descriptor.FormException, InterruptedException {
        if (template.getLaunchMethod() == LaunchMethod.SSH) {
            return createSSHSlave(template, label, mgmtUrl);
        } else if (template.getLaunchMethod() == LaunchMethod.JNLP) {
            return createJNLPSlave(template, label, mgmtUrl);
        }
        return null;
    }

    private static AnkaOnDemandSlave createJNLPSlave(AnkaCloudSlaveTemplate template, Label label, String mgmtUrl) throws InterruptedException, AnkaMgmtException, IOException, Descriptor.FormException {
//        AnkaMgmtCloud.Log("vm %s is booting...", vm.getId());
        String nodeName = generateName(template);
        String jnlpCommand = JnlpCommandBuilder.makeStartUpScript(nodeName);

        AnkaMgmtVm vm = AnkaVmFactory.getInstance().makeAnkaVm(mgmtUrl,
                template.getMasterVmId(), template.getTag(), template.getNameTemplate(), template.getSSHPort(), jnlpCommand, template.getGroup());
        vm.waitForBoot();
        AnkaMgmtCloud.Log("vm %s %s is booted, creating ssh launcher", vm.getId(), vm.getName());
        JNLPLauncher launcher = new JNLPLauncher();
        ArrayList<EnvironmentVariablesNodeProperty.Entry> a = new ArrayList<EnvironmentVariablesNodeProperty.Entry>();
        for (AnkaCloudSlaveTemplate.EnvironmentEntry e :template.getEnvironments()) {
            a.add(new EnvironmentVariablesNodeProperty.Entry(e.name, e.value));
        }

        EnvironmentVariablesNodeProperty env = new EnvironmentVariablesNodeProperty(a);
        ArrayList<NodeProperty<?>> props = new ArrayList<>();
        props.add(env);

        AnkaMgmtCloud.Log("launcher created for vm %s %s", vm.getId(), vm.getName());
        AnkaOnDemandSlave slave = new AnkaOnDemandSlave(nodeName, template.getTemplateDescription(), template.getRemoteFS(),
                template.getNumberOfExecutors(),
                template.getMode(),
                label.toString(),
                launcher,
                props, template, vm);
        return slave;
    }

    private static AnkaOnDemandSlave createSSHSlave(AnkaCloudSlaveTemplate template, Label label, String mgmtUrl) throws InterruptedException, AnkaMgmtException, IOException, Descriptor.FormException {
        AnkaMgmtVm vm = AnkaVmFactory.getInstance().makeAnkaVm(mgmtUrl,
                template.getMasterVmId(), template.getTag(), template.getNameTemplate(), template.getSSHPort(), null, template.getGroup());
        try {
            AnkaMgmtCloud.Log("vm %s is booting...", vm.getId());
            vm.waitForBoot();
            AnkaMgmtCloud.Log("vm %s %s is booted, creating ssh launcher", vm.getId(), vm.getName());
            SSHLauncher launcher = new SSHLauncher(vm.getConnectionIp(), vm.getConnectionPort(),
                    template.getCredentialsId(),
                    null, null, null, null, launchTimeoutSeconds, maxNumRetries, retryWaitTime);

            ArrayList<EnvironmentVariablesNodeProperty.Entry> a = new ArrayList<EnvironmentVariablesNodeProperty.Entry>();
            for (AnkaCloudSlaveTemplate.EnvironmentEntry e : template.getEnvironments()) {
                a.add(new EnvironmentVariablesNodeProperty.Entry(e.name, e.value));
            }

            EnvironmentVariablesNodeProperty env = new EnvironmentVariablesNodeProperty(a);
            ArrayList<NodeProperty<?>> props = new ArrayList<>();
            props.add(env);

            AnkaMgmtCloud.Log("launcher created for vm %s %s", vm.getId(), vm.getName());
            String name = vm.getName();
            AnkaOnDemandSlave slave = new AnkaOnDemandSlave(name, template.getTemplateDescription(), template.getRemoteFS(),
                    template.getNumberOfExecutors(),
                    template.getMode(),
                    label.toString(),
                    launcher,
                    props, template, vm);
            return slave;
        }
        catch (Exception e) {
            vm.terminate();
            throw e;
        }
    }


    public void setDescription(String jobAndNumber) {
        this.description = String.format("master image: %s, job name and build number: %s, vm info: (%s)",
                template.getMasterVmId(), jobAndNumber, this.vm.getInfo());

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
