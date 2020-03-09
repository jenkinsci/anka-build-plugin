package com.veertu.plugin.anka;

import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.veertu.ankaMgmtSdk.*;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import hudson.Extension;
import hudson.model.*;
import hudson.security.AccessControlled;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import hudson.slaves.SlaveComputer;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;


/**
 * Created by asafgur on 08/05/2017.
 */
public class AnkaMgmtCloud extends Cloud {


    private final List<AnkaCloudSlaveTemplate> templates;
    private static final transient java.util.logging.Logger MgmtLogger = java.util.logging.Logger.getLogger("anka-host");
    private final String ankaMgmtUrl;
    private final AnkaAPI ankaAPI;
    private final String credentialsId;
    private transient boolean eventsInit;

    private final String rootCA;

    private final boolean skipTLSVerification;

    private SaveImageRequestsHolder saveImageRequestsHolder = SaveImageRequestsHolder.getInstance();
    private InstanceDaemon daemon;
    public static Map<String, InstanceDaemon> cloudToDaemonMap;

    @DataBoundConstructor
    public AnkaMgmtCloud(String ankaMgmtUrl,
                     String cloudName,
                     String credentialsId,
                     String rootCA,
                     boolean skipTLSVerification,
                     List<AnkaCloudSlaveTemplate> templates) {
        super(cloudName);
        this.ankaMgmtUrl = ankaMgmtUrl;
        if (templates == null) {
            this.templates = Collections.emptyList();
        } else {
            this.templates = templates;
        }
        this.credentialsId = credentialsId;
        if (rootCA != null && !rootCA.isEmpty()) {
            this.rootCA = rootCA;
        } else {
            this.rootCA = null;
        }
        CertCredentials credentials = lookUpCredentials(credentialsId);
        Log("Init Anka Cloud");
        this.skipTLSVerification = skipTLSVerification;
        if (credentials != null && credentials.getClientCertificate() != null &&
                !credentials.getClientCertificate().isEmpty() && credentials.getClientKey() != null &&
                !credentials.getClientKey().isEmpty()) {
            if (ankaMgmtUrl.contains(",")) {
                String[] mgmtURLS = ankaMgmtUrl.split(",");
                ankaAPI = new AnkaAPI(Arrays.asList(mgmtURLS), skipTLSVerification, credentials.getClientCertificate() , credentials.getClientKey(), AuthType.CERTIFICATE, this.rootCA);
            } else {
                ankaAPI = new AnkaAPI(ankaMgmtUrl, skipTLSVerification, credentials.getClientCertificate() , credentials.getClientKey(), AuthType.CERTIFICATE, this.rootCA);
            }
        } else {
            if (ankaMgmtUrl.contains(",")) {
                String[] mgmtURLS = ankaMgmtUrl.split(",");
                ankaAPI = new AnkaAPI(Arrays.asList(mgmtURLS), skipTLSVerification, this.rootCA);
            } else {
                ankaAPI = new AnkaAPI(ankaMgmtUrl, skipTLSVerification, this.rootCA);
            }
        }
        initEvents();
    }

    public void initEvents() {
        if (!eventsInit) {
            if (ankaAPI != null) {
                if (cloudToDaemonMap == null)
                    cloudToDaemonMap = new HashMap<>();

                InstanceDaemon runningDaemon = cloudToDaemonMap.get(getCloudName());
                if (runningDaemon == null) {
                    if (daemon == null) {
                        daemon = new InstanceDaemon();
                    }
                    AnkaEvents.addListener(Event.nodeStarted, daemon );
                    AnkaEvents.addListener(Event.VMStarted, daemon );
                    AnkaEvents.addListener(Event.nodeTerminated, daemon );
                    AnkaEvents.addListener(Event.saveImage, daemon );
                    new Thread(daemon).start();
                }
                else if (daemon == null)
                    daemon = runningDaemon;
                cloudToDaemonMap.put(getCloudName(), daemon);
                eventsInit = true;
            }
        }
    }

    private CertCredentials lookUpCredentials(String credentialsId) {
        List<CertCredentials> credentials = lookupCredentials(CertCredentials.class, Jenkins.getInstance(), null, new ArrayList<DomainRequirement>());
        for (CertCredentials creds: credentials) {
            if (creds.getId().equals(credentialsId)) {
                return creds;
            }
        }
        return null;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public String getCloudName() {
        return name;
    }

    public String getAnkaMgmtUrl() {
        return ankaMgmtUrl;
    }

    public boolean getSkipTLSVerification() {
        return skipTLSVerification;
    }

    public String getRootCA() {
        return rootCA;
    }

    public List<AnkaVmTemplate> listVmTemplates() {
        if (ankaAPI == null) {
            return new ArrayList<>();
        }
        try {
            return ankaAPI.listTemplates();
        } catch (AnkaMgmtException e) {
            e.printStackTrace();
            Log("Problem connecting to Anka mgmt host");
            return new ArrayList<AnkaVmTemplate>();
        }
    }

    public List<String> getTemplateTags(String masterVmId) {
        if (ankaAPI == null) {
            return new ArrayList<>();
        }
        try {
            return ankaAPI.listTemplateTags(masterVmId);
        } catch (AnkaMgmtException e) {
            e.printStackTrace();
            Log("Problem connecting to Anka mgmt host");
            return new ArrayList<>();
        }
    }

    public List<AnkaCloudSlaveTemplate> getTemplates() { return templates; }

    public List<NodeGroup> getNodeGroups() {
        if (ankaAPI == null) {
            return new ArrayList<>();
        }
        try{
            return ankaAPI.getNodeGroups();
        } catch (AnkaMgmtException e) {
            e.printStackTrace();
            Log("Problem connecting to Anka mgmt host");
            return new ArrayList<>();
        }
    }

    @Override
    public Collection<NodeProvisioner.PlannedNode> provision(Label label, int excessWorkload) {
        initEvents();
        List<NodeProvisioner.PlannedNode> plannedNodes = new ArrayList<>();

        final AnkaCloudSlaveTemplate t = getTemplate(label);
        Log("Attempting to provision slave from template " + t + " needed by excess workload of " + excessWorkload + " units of label '" + label + "'");
        if (label == null || t == null) {
            Log("can't start an on demand instance without a label");
            return Collections.emptyList();
        }

        while (excessWorkload > 0) {
            // check that mgmt server has this template
            if (!hasMasterVm(t.getMasterVmId())) {
                Log("no such template %s", t.getMasterVmId());
                break;
            }
            try {
                NodeProvisioner.PlannedNode newNode = AnkaPlannedNode.createInstance(this, t);
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
        initEvents();
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

    public AnkaAPI getAnkaApi() {
        return ankaAPI;
    }

    public boolean isOnline() {
        try {
            AnkaCloudStatus status = ankaAPI.getStatus();
            if (status.getStatus().toLowerCase().equals("running")) {
                return true;
            }
            return false;
        } catch (AnkaMgmtException e) {
            return false;
        }
    }

    public Boolean isPushSupported() {
        try {
            this.getAnkaApi().getImageRequests();
            return true;
        } catch (AnkaNotFoundException e) {
            return false;
        } catch (AnkaMgmtException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<AnkaMgmtCloud> getAnkaClouds() {
        List<AnkaMgmtCloud> clouds = new ArrayList<AnkaMgmtCloud>();
        final Jenkins jenkins = Jenkins.getInstance();
        for (Cloud cloud : jenkins.clouds) {
            if (cloud instanceof AnkaMgmtCloud) {
                AnkaMgmtCloud ankaCloud = (AnkaMgmtCloud) cloud;
                clouds.add(ankaCloud);
            }
        }
        return clouds;
    }

    public static AnkaMgmtCloud getCloudThatHasImage(String masterVMID) {
        final Jenkins jenkins = Jenkins.getInstance();
        for (Cloud cloud : jenkins.clouds) {
            if (cloud instanceof AnkaMgmtCloud) {
                AnkaMgmtCloud ankaCloud = (AnkaMgmtCloud) cloud;
                if (ankaCloud.hasMasterVm(masterVMID)) {
                    return ankaCloud;
                }
            }
        }
        return null;
    }

    public AnkaOnDemandSlave StartNewDynamicSlave(DynamicSlaveProperties properties, String label) throws InterruptedException, IOException, Descriptor.FormException, AnkaMgmtException, ExecutionException {
        AnkaOnDemandSlave dynamicSlave = DynamicSlave.createDynamicSlave(this, properties, label);
        return dynamicSlave;
    }

    public AnkaMgmtVm startVMInstance(String templateId,
                                      String tag, String nameTemplate, int sshPort, String startUpScript,
                                      String groupId, int priority, String name, String externalId) throws AnkaMgmtException {
        try {
            AnkaMgmtVm vm = ankaAPI.makeAnkaVm(templateId, tag, nameTemplate, sshPort,
                    startUpScript, groupId, priority, name, externalId\);
            AnkaEvents.fire(Event.VMStarted, new VMStarted(vm));
            return vm;
        } catch (AnkaMgmtException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void nodeStarted(AbstractAnkaSlave node) {
        AnkaEvents.fire(Event.nodeStarted, new NodeStarted(node));
    }

    public void saveImage(AbstractAnkaSlave node) throws AnkaMgmtException {
        AnkaEvents.fire(Event.saveImage, new SaveImageEvent(node));
        ImageSaver.saveImage(this, node, node.getVM());
    }

    public void nodeTerminated(AbstractAnkaSlave node){
        AnkaEvents.fire(Event.nodeTerminated, new NodeTerminated(node));
    }


    @Extension
    public static final class DescriptorImpl extends Descriptor<Cloud> {

        @Override
        public String getDisplayName() {
            return "Anka Cloud";
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context) {
            if (!(context instanceof AccessControlled ? (AccessControlled) context : Jenkins.getInstance()).hasPermission(Computer.CONFIGURE)) {
                return new ListBoxModel();
            }
            final List<CertCredentials> credentials;
            credentials = lookupCredentials(CertCredentials.class, Jenkins.getInstance(), null, new ArrayList<DomainRequirement>());
            ListBoxModel listBox = new StandardUsernameListBoxModel();
            for (CertCredentials cred: credentials) {
                listBox.add(cred.getName(), cred.getId());
            }
            return listBox;
        }

    }

}
