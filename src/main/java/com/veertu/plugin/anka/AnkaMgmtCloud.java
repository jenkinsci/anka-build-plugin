package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import com.veertu.ankaMgmtSdk.AnkaVmFactory;
import com.veertu.ankaMgmtSdk.AnkaVmTemplate;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import hudson.Extension;
import hudson.model.*;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import hudson.slaves.SlaveComputer;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;


/**
 * Created by asafgur on 08/05/2017.
 */
public class AnkaMgmtCloud extends Cloud {


    private final String cloudName;
    private final List<AnkaCloudSlaveTemplate> templates;
    private static java.util.logging.Logger MgmtLogger = java.util.logging.Logger.getLogger("anka-host");
    private final String ankaMgmtUrl;



    private final String ankaMgmtPort;

    @DataBoundConstructor
    public AnkaMgmtCloud(String ankaMgmtUrl, String ankaMgmtPort,
                     String cloudName,
                     List<AnkaCloudSlaveTemplate> templates) {
        super(cloudName);
        this.cloudName = cloudName;
        this.ankaMgmtUrl = ankaMgmtUrl;
        this.ankaMgmtPort = ankaMgmtPort;
        if (templates == null) {
            this.templates = Collections.emptyList();
        } else {
            this.templates = templates;
        }
        Log("Init Anka Cloud");
    }

    public String getCloudName() {
        return cloudName;
    }

    public String getAnkaMgmtPort() {
        return ankaMgmtPort;
    }

    public String getAnkaMgmtUrl() {
        return ankaMgmtUrl;
    }

    public List<AnkaVmTemplate> listVmTemplates() {
        try {
            return AnkaVmFactory.getInstance().listTemplates(this.ankaMgmtUrl, this.ankaMgmtPort);
        } catch (AnkaMgmtException e) {
            e.printStackTrace();
            Log("Problem connecting to Anka mgmt host");
            return new ArrayList<AnkaVmTemplate>();
        }
    }

    public List<String> getTemplateTags(String masterVmId) {
        try {
            return AnkaVmFactory.getInstance().listTemplateTags(this.ankaMgmtUrl, this.ankaMgmtPort, masterVmId);
        } catch (AnkaMgmtException e) {
            e.printStackTrace();
            Log("Problem connecting to Anka mgmt host");
            return new ArrayList<>();
        }
    }

    public List<AnkaCloudSlaveTemplate> getTemplates() { return templates; }

    @Override
    public Collection<NodeProvisioner.PlannedNode> provision(Label label, int excessWorkload) {
        List<NodeProvisioner.PlannedNode> plannedNodes = new ArrayList<>();

        final AnkaCloudSlaveTemplate t = getTemplate(label);
        Log("Attempting to provision slave from template " + t + " needed by excess workload of " + excessWorkload + " units of label '" + label + "'");
        if (label == null || t == null) {
            Log("can't start an on demand instance without a label");
            return Collections.emptyList();
        }

        while (excessWorkload > 0) {
            AnkaMgmtVm vm = null;

            // check that mgmt server has this template
            if (!hasMasterVm(t.getMasterVmId())) {
                Log("no such template %s", t.getMasterVmId());
                break;
            }
            try {
                vm = AnkaVmFactory.getInstance().makeAnkaVm(this.ankaMgmtUrl, this.ankaMgmtPort,
                        t.getMasterVmId(), t.getTag(), t.getSSHPort());
                NodeProvisioner.PlannedNode newNode = AnkaPlannedNode.createInstance(t, label, vm);
                plannedNodes.add(newNode);
                excessWorkload -= t.getNumberOfExecutors();
            }
            catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        return plannedNodes;
    }

    public AnkaCloudSlaveTemplate getTemplate(final Label label) {

        for (AnkaCloudSlaveTemplate t : this.templates) {

            if (t.getMode() == Node.Mode.NORMAL) {

                if (label == null || label.matches(t.getLabelSet())) {
                    return t;
                }
            } else if (t.getMode() == Node.Mode.EXCLUSIVE) {

                if (label != null && label.matches(t.getLabelSet())) {
                    return t;
                }
            }
        }
        return null;
    }

    private boolean hasMasterVm(String templateId) {
        for (AnkaVmTemplate t: this.listVmTemplates()){
            if (t.getId().equals(templateId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canProvision(Label label) {
        AnkaCloudSlaveTemplate template = getTemplateFromLabel(label);
        if (template == null){
            return false;
        }
        return true;
    }


    public AnkaCloudSlaveTemplate getTemplateFromLabel(final Label label) {

        for (AnkaCloudSlaveTemplate t : this.templates) {

            if (t.getMode() == Node.Mode.NORMAL) {

                if (label == null || label.matches(t.getLabelSet())) {
                    return t;
                }
            } else if (t.getMode() == Node.Mode.EXCLUSIVE) {

                if (label != null && label.matches(t.getLabelSet())) {
                    return t;
                }
            }
        }
        return null;
    }

//    private AnkaMgmtCommunicator GetAnkaMgmtCommunicator() {
//
//    }


    private static void InternalLog(Slave slave, SlaveComputer slaveComputer, TaskListener listener, String format, Object... args) {
        String s = "";
        if (slave != null)
            s = String.format("[%s] ", slave.getNodeName());
        if (slaveComputer != null)
            s = String.format("[%s] ", slaveComputer.getName());
        s = s + String.format(format, args);
        s = s + "\n";
        if (listener != null)
            listener.getLogger().print(s);
        MgmtLogger.log(Level.INFO, s);
    }



    public static void Log(String msg) {
        InternalLog(null, null, null, msg, null);
    }

    public static void Log(String format, Object... args) {
        InternalLog(null, null, null, format, args);
    }


    public static void Log(Slave slave, TaskListener listener, String msg) {
        InternalLog(slave, null, listener, msg, null);
    }

    public static void Log(SlaveComputer slave, TaskListener listener, String format, Object... args) {
        InternalLog(null, slave, listener, format, args);
    }


    @Extension
    public static final class DescriptorImpl extends Descriptor<Cloud> {

        @Override
        public String getDisplayName() {
            return "Anka Cloud";
        }
    }

}
