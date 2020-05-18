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
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static java.lang.Thread.sleep;


/**
 * Created by asafgur on 08/05/2017.
 */
public class AnkaMgmtCloud extends Cloud {


    private final List<AnkaCloudSlaveTemplate> templates;
    private static final transient java.util.logging.Logger MgmtLogger = java.util.logging.Logger.getLogger("anka-host");
    private final String ankaMgmtUrl;
    private final AnkaAPI ankaAPI;
    private final String credentialsId;

    private final String rootCA;

    private final boolean skipTLSVerification;
    private int   cloudInstanceCap;
    private transient ReentrantLock nodeNumLock = new ReentrantLock();
    private transient SaveImageRequestsHolder saveImageRequestsHolder = SaveImageRequestsHolder.getInstance();
    private int vmPollTime;

    public int getMonitorRecurrenceMinutes() {
        return AnkaSlaveMonitor.getMonitorRecurrenceMinutes();
    }

    @DataBoundSetter
    public void setMonitorRecurrenceMinutes(int minutes) {
        AnkaSlaveMonitor.setMonitorRecurrenceMinutes(minutes);
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    @DataBoundSetter
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    protected int maxConnections = 50;

    public int getConnectionKeepAliveSeconds() {
        return connectionKeepAliveSeconds;
    }

    @DataBoundSetter
    public void setConnectionKeepAliveSeconds(int connectionKeepAliveSeconds) {
        this.connectionKeepAliveSeconds = connectionKeepAliveSeconds;
    }

    protected int connectionKeepAliveSeconds = 120;

    @DataBoundConstructor
    public AnkaMgmtCloud(String ankaMgmtUrl,
                     String cloudName,
                     String credentialsId,
                     String rootCA,
                     boolean skipTLSVerification,
                     List<AnkaCloudSlaveTemplate> templates, int cloudInstanceCap) {
        super(cloudName);
        if (cloudInstanceCap < 0) {
            this.cloudInstanceCap = 0; // zero means unlimited
        }
        this.cloudInstanceCap = cloudInstanceCap;
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
        this.ankaAPI.setMaxConnections(maxConnections);
        this.ankaAPI.setConnectionKeepAliveSeconds(connectionKeepAliveSeconds);
    }

    public static void markFuture(AnkaMgmtCloud cloud, AbstractAnkaSlave abstractAnkaSlave) {
        ImageSaver.markFuture(cloud, abstractAnkaSlave);
    }

    protected Object readResolve() {
        this.nodeNumLock = new ReentrantLock();
        this.ankaAPI.setConnectionKeepAliveSeconds(connectionKeepAliveSeconds);
        this.ankaAPI.setMaxConnections(maxConnections);
        return this;
    }



    private CertCredentials lookUpCredentials(String credentialsId) {
        List<CertCredentials> credentials = lookupCredentials(CertCredentials.class, Jenkins.get(), null, new ArrayList<DomainRequirement>());
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

    public int getCloudInstanceCap() {
        return cloudInstanceCap;
    }

    public void setCloudInstanceCap(int val) {
        cloudInstanceCap = val;
    }

    public int getNumOfRunningNodes() {
        int numRunningNodes = 0;
        for (Node node : Jenkins.get().getNodes()) {
            if (node instanceof AbstractAnkaSlave) {
                AbstractAnkaSlave ankaNode = (AbstractAnkaSlave)node;
                if (ankaNode.getCloud().getCloudName().equals(this.getCloudName())) {
                    numRunningNodes++;
                }
            }
        }
        return numRunningNodes;
    }

    public NodeCountResponse getNumOfRunningNodesPerLabel(Label label) {
        AnkaCloudSlaveTemplate templateFromLabel = getTemplateFromLabel(label);
        int numRunningNodes = 0;
        int runningTemplateNodes = 0;
        if (templateFromLabel != null) {
            for (Node node : Jenkins.get().getNodes()) {
                if (node instanceof AbstractAnkaSlave) {
                    AbstractAnkaSlave ankaNode = (AbstractAnkaSlave)node;
                    if (ankaNode.getCloud().getCloudName().equals(this.getCloudName())) {
                        if (label.matches(ankaNode.getTemplate().getLabelSet())) {
                            runningTemplateNodes++;
                        }
                        numRunningNodes++;
                    }
                }
            }
        }
        return new NodeCountResponse(numRunningNodes, runningTemplateNodes);
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
        List<NodeProvisioner.PlannedNode> plannedNodes = new ArrayList<>();

        Jenkins jenkinsInstance = Jenkins.get();
        if (jenkinsInstance.isQuietingDown() || jenkinsInstance.isTerminating()) {
            Log("Not provisioning nodes, Jenkins instance is terminating or quieting down");
            return Collections.emptyList();
        }

        final AnkaCloudSlaveTemplate t = getTemplate(label);
        Log("Attempting to provision slave from template " + t + " needed by excess workload of " + excessWorkload + " units of label '" + label + "'");
        if (label == null || t == null) {
            Log("can't start an on demand instance without a label");
            return Collections.emptyList();
        }
        try {
            int number = Math.max(excessWorkload / t.getNumberOfExecutors(), 1);
            try {
                this.nodeNumLock.lock();
                if (cloudInstanceCap > 0 || t.getInstanceCapacity() > 0) {
                    NodeCountResponse nodeCount = getNumOfRunningNodesPerLabel(label);
                    if (cloudInstanceCap > 0) {
                        int allowedCloudCapacity = cloudInstanceCap - nodeCount.numNodes;
                        if (allowedCloudCapacity <= 0) {
                            return plannedNodes;
                        }
                        if (number > allowedCloudCapacity) {
                            number = allowedCloudCapacity;
                        }
                    }
                    if (t.getInstanceCapacity() > 0) {
                        int allowedTemplateCapacity = cloudInstanceCap - nodeCount.numNodesPerLabel;
                        if (allowedTemplateCapacity <= 0) {
                            return plannedNodes;
                        }
                        if (number > allowedTemplateCapacity) {
                            number = allowedTemplateCapacity;
                        }
                    }
                }
                final List<AbstractAnkaSlave> slaves = createNewSlaves(t, number);

                if (slaves == null || slaves.isEmpty()) {
                    Log("Can't raise nodes for " + t);
                    return Collections.emptyList();
                }

                for (final AbstractAnkaSlave slave : slaves) {
                    if (slave == null) {
                        Log("Can't raise node for " + t);
                        continue;
                    }

                    plannedNodes.add(AnkaPlannedNodeCreator.createPlannedNode(this, t, slave));
                }
                return plannedNodes;
            } finally {
                this.nodeNumLock.unlock();
            }
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<AbstractAnkaSlave> createNewSlaves(AnkaCloudSlaveTemplate template, int number) throws AnkaMgmtException, IOException, Descriptor.FormException {
        List<AbstractAnkaSlave> newSlaves = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            String nodeName = AnkaOnDemandSlave.generateName(template);
            String startUpScript = AnkaOnDemandSlave.createStartUpScript(template, nodeName);

            AnkaOnDemandSlave slave = null;
            String newInstanceId = null;
            try {
                newInstanceId = ankaAPI.startVM(template.getMasterVmId(), template.getTag(), startUpScript,
                        template.getGroup(), template.getPriority(),
                        nodeName, AnkaOnDemandSlave.getJenkinsNodeLink(nodeName));
                AnkaLauncher launcher = new AnkaLauncher(this, template, newInstanceId);
                slave = new AnkaOnDemandSlave(this, nodeName, template.getDescription(),
                        template.getRemoteFS(), template.getNumberOfExecutors(), template.getMode(),
                        template.getLabelString(), launcher, template.getNodeProperties(), template, newInstanceId);
                newSlaves.add(slave);
                Jenkins.get().addNode(slave); // add our node as early as possible to avoid zombies
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // insurance that our node is in the jenkins loop
                if (slave == null) {
                    Log("Failed to create Node "+ nodeName + ". terminating "+ newInstanceId);
                    terminateVMInstance(newInstanceId);
//                }
                } else {
                    Node nodeFromJenkins = Jenkins.get().getNode(nodeName);
                    if (nodeFromJenkins == null) { // shouldn't happen
                        Log("Node " + slave.getNodeName() + " is not attached to jenkins... terminating");
                        slave.terminate(); // but if it does, then terminate
                    }
                }
            }
        }
        return newSlaves;
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
        if (template.getInstanceCapacity() > 0 || cloudInstanceCap > 0) {
            try {
                this.nodeNumLock.lock();
                NodeCountResponse countResponse = getNumOfRunningNodesPerLabel(label);
                if ((cloudInstanceCap > 0 && countResponse.numNodes >= cloudInstanceCap)
                        ||
                    (template.getInstanceCapacity() >0 &&
                        countResponse.numNodesPerLabel >= template.getInstanceCapacity())) {
                    return false;
                }
            } finally {
                this.nodeNumLock.unlock();
            }
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
        final Jenkins jenkins = Jenkins.get();
        for (Cloud cloud : jenkins.clouds) {
            if (cloud instanceof AnkaMgmtCloud) {
                AnkaMgmtCloud ankaCloud = (AnkaMgmtCloud) cloud;
                clouds.add(ankaCloud);
            }
        }
        return clouds;
    }

    public static AnkaMgmtCloud getCloudThatHasImage(String masterVMID) {
        final Jenkins jenkins = Jenkins.get();
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
        AnkaCloudSlaveTemplate template = properties.toSlaveTemplate(label);
        List<AbstractAnkaSlave> newSlaves = createNewSlaves(template, 1);
        for (AbstractAnkaSlave slave: newSlaves) {
            AnkaPlannedNodeCreator.createPlannedNode(this, template, slave);
            Jenkins.get().addNode(slave);
            return (AnkaOnDemandSlave) slave;
        }
        return null;
    }



    public void saveImage(AbstractAnkaSlave node) throws AnkaMgmtException {
        ImageSaver.saveImage(this, node);
    }


    public void updateInstance(String vmId, String name, String jenkinsNodeLink, String jobIdentifier) throws AnkaMgmtException {
        ankaAPI.updateInstance(vmId, name, jenkinsNodeLink, jobIdentifier);
    }

    public void terminateVMInstance(String id) throws AnkaMgmtException {
        AnkaVmInstance ankaVmInstance = ankaAPI.showInstance(id);

        if (ankaVmInstance == null || ankaVmInstance.isTerminatingOrTerminated()) {
            return; // if it's already terminated just forget about it
        }

        ankaAPI.terminateInstance(id);
        try {
            sleep(200);
        } catch (InterruptedException e) {
            // no rest for the wicked
        }

        while(ankaVmInstance != null && !ankaVmInstance.isTerminatingOrTerminated()) {
            ankaAPI.terminateInstance(id);
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                // no rest for the wicked
            }
            ankaVmInstance = ankaAPI.showInstance(id);
        }
    }

    public AnkaVmInstance showInstance(String id) throws AnkaMgmtException {
        return ankaAPI.showInstance(id);
    }

    public static AnkaMgmtCloud get(String cloudName) {
        return (AnkaMgmtCloud) Jenkins.get().getCloud(cloudName);
    }

    public int getVmPollTime() {
        return this.vmPollTime;
    }

    @DataBoundSetter
    public void setVmPollTime(int milliseconds) {
        vmPollTime =milliseconds;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Cloud> {

        @Override
        public String getDisplayName() {
            return "Anka Cloud";
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context) {
            if (!(context instanceof AccessControlled ? (AccessControlled) context : Jenkins.get()).hasPermission(Computer.CONFIGURE)) {
                return new ListBoxModel();
            }
            final List<CertCredentials> credentials;
            credentials = lookupCredentials(CertCredentials.class, Jenkins.get(), null, new ArrayList<DomainRequirement>());
            ListBoxModel listBox = new StandardUsernameListBoxModel();
            for (CertCredentials cred: credentials) {
                listBox.add(cred.getName(), cred.getId());
            }
            return listBox;
        }

    }

}
