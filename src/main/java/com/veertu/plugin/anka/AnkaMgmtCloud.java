package com.veertu.plugin.anka;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
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
import jenkins.slaves.iterators.api.NodeIterator;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import static java.lang.Thread.sleep;

public class AnkaMgmtCloud extends Cloud {
    private static final java.util.logging.Logger MgmtLogger = java.util.logging.Logger.getLogger("anka-host");
    private final List<AnkaCloudSlaveTemplate> templates;
    private final String ankaMgmtUrl;
    private final String credentialsId;
    private final String rootCA;
    private final boolean skipTLSVerification;
    protected int maxConnections = 50;
    protected int connectionKeepAliveSeconds = 120;
    private transient AnkaAPI ankaAPI;
    private int cloudInstanceCap;
    private transient ReentrantLock nodeNumLock = new ReentrantLock();
    private int vmPollTime;
    private transient List<DynamicSlaveTemplate> dynamicTemplates;
    private int launchTimeout;
    private int maxLaunchRetries;
    private int launchRetryWaitTime;
    private int sshLaunchDelaySeconds;
    private int vmIPAssignWaitSeconds;
    private int vmIPAssignRetries;
    private String durabilityMode = "durable";

    @DataBoundConstructor
    public AnkaMgmtCloud(String ankaMgmtUrl, String cloudName, String credentialsId, String rootCA, boolean skipTLSVerification, List<AnkaCloudSlaveTemplate> templates, int cloudInstanceCap) {
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
        if (rootCA != null && !rootCA.trim().isEmpty()) {
            this.rootCA = rootCA;
        } else {
            this.rootCA = null;
        }

        this.dynamicTemplates = Collections.synchronizedList(new ArrayList<>());

        Log("Init Anka Cloud");
        this.skipTLSVerification = skipTLSVerification;

        createAnkaAPIObject();
    }

    public static void markFuture(AnkaMgmtCloud cloud, AbstractAnkaSlave abstractAnkaSlave) {
        ImageSaver.markFuture(cloud, abstractAnkaSlave);
    }

    private static void InternalLog(Slave slave, SlaveComputer slaveComputer, TaskListener listener, String format, Object... args) {
        String s = "";
        if (slave != null) s = String.format("[%s] ", slave.getNodeName());
        if (slaveComputer != null) s = String.format("[%s] ", slaveComputer.getName());
        s = s + String.format(format, args);
        s = s + "\n";
        if (listener != null) listener.getLogger().print(s);
        MgmtLogger.log(Level.INFO, s);
    }

    public static void Log(String msg) {
        InternalLog(null, null, null, msg);
    }

    public static void Log(String format, Object... args) {
        InternalLog(null, null, null, format, args);
    }

    public static void Log(Slave slave, TaskListener listener, String msg) {
        InternalLog(slave, null, listener, msg);
    }

    public static void Log(SlaveComputer slave, TaskListener listener, String format, Object... args) {
        InternalLog(null, slave, listener, format, args);
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

    public static AnkaMgmtCloud get(String cloudName) {
        return (AnkaMgmtCloud) Jenkins.get().getCloud(cloudName);
    }

    public int getLaunchTimeout() {
        if (launchTimeout <= 0) {
            return AnkaLauncher.defaultLaunchTimeoutSeconds;
        }
        return launchTimeout;
    }

    @DataBoundSetter
    public void setLaunchTimeout(int launchTimeout) {
        this.launchTimeout = launchTimeout;
    }

    public int getMaxLaunchRetries() {
        if (maxLaunchRetries <= 0) {
            return AnkaLauncher.defaultMaxNumRetries;
        }
        return maxLaunchRetries;
    }

    @DataBoundSetter
    public void setMaxLaunchRetries(int maxLaunchRetries) {
        this.maxLaunchRetries = maxLaunchRetries;
    }

    public int getLaunchRetryWaitTime() {
        if (launchRetryWaitTime <= 0) {
            return AnkaLauncher.defaultRetryWaitTime;
        }
        return launchRetryWaitTime;
    }

    @DataBoundSetter
    public void setLaunchRetryWaitTime(int launchRetryWaitTime) {
        this.launchRetryWaitTime = launchRetryWaitTime;
    }

    public int getSshLaunchDelaySeconds() {
        if (sshLaunchDelaySeconds <= 0) {
            return AnkaLauncher.defaultSSHLaunchDelay;
        }
        return sshLaunchDelaySeconds;
    }

    @DataBoundSetter
    public void setSshLaunchDelaySeconds(int sshLaunchDelaySeconds) {
        this.sshLaunchDelaySeconds = sshLaunchDelaySeconds;
    }

    public int getVmIPAssignWaitSeconds() {
        if (vmIPAssignWaitSeconds <= 0) {
            return AnkaLauncher.defaultVmIPAssignWaitSeconds;
        }
        return vmIPAssignWaitSeconds;
    }

    @DataBoundSetter
    public void setVmIPAssignWaitSeconds(int vmIPAssignWaitSeconds) {
        this.vmIPAssignWaitSeconds = vmIPAssignWaitSeconds;
    }

    public int getVmIPAssignRetries() {
        if (vmIPAssignRetries <= 0) {
            return AnkaLauncher.defaultVmIPAssignRetries;
        }
        return vmIPAssignRetries;
    }

    @DataBoundSetter
    public void setVmIPAssignRetries(int vmIPAssignRetries) {
        this.vmIPAssignRetries = vmIPAssignRetries;
    }

    public String getDurabilityMode() {
        return durabilityMode;
    }

    @DataBoundSetter
    public void setDurabilityMode(String durabilityMode) {
        this.durabilityMode = durabilityMode;
    }

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

    public int getConnectionKeepAliveSeconds() {
        return connectionKeepAliveSeconds;
    }

    @DataBoundSetter
    public void setConnectionKeepAliveSeconds(int connectionKeepAliveSeconds) {
        this.connectionKeepAliveSeconds = connectionKeepAliveSeconds;
    }

    private void createAnkaAPIObject() {
        if (maxConnections == 0) {
            maxConnections = 50;
        }
        if (connectionKeepAliveSeconds == 0) {
            connectionKeepAliveSeconds = 120;
        }
        if (vmPollTime <= 0) {
            vmPollTime = 5000;
        }

        String[] mgmtURLS;
        if (ankaMgmtUrl.contains(",")) {
            mgmtURLS = ankaMgmtUrl.split(",");
        } else {
            mgmtURLS = new String[]{ankaMgmtUrl};
        }

        Credentials credentials = CredentialsProvider.lookupCredentialsInItemGroup(
                        Credentials.class,
                        Jenkins.get(),
                        null,
                        null
                ).stream()
                .filter(c -> {
                    if (c instanceof IdCredentials) {
                        return ((IdCredentials) c).getId().equals(credentialsId);
                    } else if (c instanceof CertCredentials) {
                        return ((CertCredentials) c).getId().equals(credentialsId);
                    }

                    return false;
                })
                .findFirst()
                .orElse(null);

        if (credentials instanceof StringCredentialsImpl) {
            StringCredentialsImpl stringCreds = (StringCredentialsImpl) credentials;
            String secret = stringCreds.getSecret().getPlainText();

            ankaAPI = new AnkaAPI(List.of(mgmtURLS), skipTLSVerification, this.rootCA, stringCreds.getId(), secret);

        } else if (credentials instanceof StandardUsernamePasswordCredentials) {
            StandardUsernamePasswordCredentials userPassCreds = (StandardUsernamePasswordCredentials) credentials;
            String secret = userPassCreds.getPassword().getPlainText();
            ankaAPI = new AnkaAPI(List.of(mgmtURLS), skipTLSVerification, this.rootCA, userPassCreds.getUsername(), secret);

        } else if (credentials instanceof CertCredentials) {
            CertCredentials certCredentials = (CertCredentials) credentials;
            if (certCredentials.getClientCertificate() == null
                    || certCredentials.getClientCertificate().isEmpty()
                    || certCredentials.getClientKey() == null
                    || certCredentials.getClientKey().isEmpty()) {
                throw new IllegalArgumentException("Both client certificate and key must be provided for certificate authentication");
            }

            ankaAPI = new AnkaAPI(List.of(mgmtURLS),
                    skipTLSVerification,
                    certCredentials.getClientCertificate(),
                    certCredentials.getClientKey(),
                    AuthType.CERTIFICATE,
                    this.rootCA);

        } else {
            ankaAPI = new AnkaAPI(Arrays.asList(mgmtURLS), skipTLSVerification, this.rootCA);
        }

        ankaAPI.setMaxConnections(maxConnections);
        ankaAPI.setConnectionKeepAliveSeconds(connectionKeepAliveSeconds);
    }

    /**
     * Checks if the configured credentials are legacy UAK credentials stored as StringCredentials
     * and logs a deprecation warning to encourage users to migrate to UsernamePasswordCredentials.
     */
    private void checkAndWarnLegacyUakCredentials() {
        if (credentialsId == null || credentialsId.trim().isEmpty()) {
            return;
        }

        StringCredentialsImpl stringCreds = CredentialsProvider.lookupCredentialsInItemGroup(
                        StringCredentialsImpl.class,
                        Jenkins.get(),
                        null,
                        null
                ).stream()
                .filter(c -> c.getId().equals(credentialsId))
                .findFirst()
                .orElse(null);

        if (stringCreds != null && isLegacyUakCredential(stringCreds)) {
            Log("WARNING: DEPRECATED: Anka Cloud '%s' is using \"Secret text\" credential type for UAK authentication. " +
                            "Please migrate to \"Username with Password\" credential where Username=UAK_ID and Password=UAK_SECRET. " +
                            "\"Secret text\" support for UAK will be removed in future.",
                    getCloudName());
        }
    }

    /**
     * Determines if a StringCredentials instance is likely a legacy UAK credential
     * by checking if the secret is a valid UAK private key (PEM format or concatenated string).
     */
    private boolean isLegacyUakCredential(StringCredentialsImpl stringCreds) {
        try {
            String secret = stringCreds.getSecret().getPlainText();
            UakAuthenticator.getRSAPrivateKey(secret);
            return true;
        } catch (Exception e) {
            // If we can't access the secret or it's not a valid RSA key, assume it's not a UAK credential
            return false;
        }
    }

    protected Object readResolve() {
        this.nodeNumLock = new ReentrantLock();

        // Check for legacy UAK credentials and warn users
        checkAndWarnLegacyUakCredentials();
        
        createAnkaAPIObject();
        if (this.dynamicTemplates == null) {
            this.dynamicTemplates = Collections.synchronizedList(new ArrayList<>());
        }
        return this;
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

    public int getCloudCapacity() {
        if (cloudInstanceCap < 0) { // smaller than 0 is unlimited
            return Integer.MAX_VALUE;
        }
        if (cloudInstanceCap > 0) {
            return cloudInstanceCap;
        }
        if (ankaAPI != null) {
            try {
                return ankaAPI.getCloudCapacity(); // automatic cloud capacity
            } catch (AnkaMgmtException e) {
                return 1;
            }
        }
        return 0;
    }

    public NodeCountResponse getNumOfRunningNodesPerLabel(Label label) {
        AnkaCloudSlaveTemplate templateFromLabel = getTemplate(label);
        int numRunningNodes = 0;
        int runningTemplateNodes = 0;
        if (templateFromLabel != null) {
            for (AbstractAnkaSlave ankaNode : NodeIterator.nodes(AbstractAnkaSlave.class)) {
                if (ankaNode.getCloud().getCloudName().equals(this.getCloudName())) {
                    if (label.matches(ankaNode.getTemplate().getLabelSet())) {
                        runningTemplateNodes++;
                    }
                    numRunningNodes++;
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

    public List<AnkaCloudSlaveTemplate> getTemplates() {
        return templates;
    }

    public List<DynamicSlaveTemplate> getDynamicTemplates() {
        // Returns a copy of the array since SynchronizedList is not iteration safe

        synchronized (this.dynamicTemplates) {
            return new ArrayList<>(this.dynamicTemplates);
        }
    }

    public List<NodeGroup> getNodeGroups() {
        if (ankaAPI == null) {
            return new ArrayList<>();
        }
        try {
            return ankaAPI.getNodeGroups();
        } catch (AnkaMgmtException e) {
            e.printStackTrace();
            Log("Problem connecting to Anka mgmt host");
            return new ArrayList<>();
        }
    }

    @Override
    public Collection<NodeProvisioner.PlannedNode> provision(CloudState state, int excessWorkload) {
        Label label = state.getLabel();
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
                int cloudCapacity = getCloudCapacity();
                this.nodeNumLock.lock();
                if (cloudCapacity > 0 || t.getInstanceCapacity() > 0) {
                    NodeCountResponse nodeCount = getNumOfRunningNodesPerLabel(label);
                    if (cloudCapacity > 0) {
                        int allowedCloudCapacity = cloudCapacity - nodeCount.numNodes;
                        if (allowedCloudCapacity <= 0) {
                            return plannedNodes;
                        }
                        if (number > allowedCloudCapacity) {
                            number = allowedCloudCapacity;
                        }
                    }
                    if (t.getInstanceCapacity() > 0) {
                        int allowedTemplateCapacity = t.getInstanceCapacity() - nodeCount.numNodesPerLabel;
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
        if (durabilityMode == null || durabilityMode.equalsIgnoreCase(DurabilityMode.Durable)) {
            return createNewDurableSlaves(template, number);
        } else {
            return createNewLightWeightSlaves(template, number);
        }
    }

    private List<AbstractAnkaSlave> createNewDurableSlaves(AnkaCloudSlaveTemplate template, int number) throws AnkaMgmtException, IOException, Descriptor.FormException {
        List<AbstractAnkaSlave> newSlaves = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            String nodeName = AnkaOnDemandSlave.generateName(template);
            String startUpScript = AnkaOnDemandSlave.createStartUpScript(template, nodeName);

            AnkaOnDemandSlave slave = null;
            String newInstanceId = null;
            try {
                newInstanceId = ankaAPI.startVM(template.getMasterVmId(), template.getTag(), startUpScript, template.getGroup(), template.getPriority(), nodeName, AnkaOnDemandSlave.getJenkinsNodeLink(nodeName), template.getVcpu(), template.getVram());
                AnkaLauncher launcher = new AnkaLauncher(this, template, newInstanceId, this.launchTimeout, this.maxLaunchRetries, this.launchRetryWaitTime, this.sshLaunchDelaySeconds, this.vmIPAssignWaitSeconds, this.vmIPAssignRetries);
                slave = new AnkaOnDemandSlave(this, nodeName, template.getDescription(), template.getRemoteFS(), template.getNumberOfExecutors(), template.getMode(), template.getLabelString(), launcher, template.getNodeProperties(), template, newInstanceId);
                newSlaves.add(slave);
                Jenkins.get().addNode(slave); // add our node as early as possible to avoid zombies
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // insurance that our node is in the jenkins loop
                if (slave == null) {
                    Log("Failed to create Node " + nodeName + ". terminating " + newInstanceId);
                    terminateVMInstance(newInstanceId);
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

    private List<AbstractAnkaSlave> createNewLightWeightSlaves(AnkaCloudSlaveTemplate template, int number) throws AnkaMgmtException, IOException, Descriptor.FormException {
        List<AbstractAnkaSlave> newSlaves = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            String nodeName = AnkaOnDemandSlave.generateName(template);
            String startUpScript = AnkaOnDemandSlave.createStartUpScript(template, nodeName);

            AnkaOnDemandSlave slave = null;
            String newInstanceId = null;
            try {
                newInstanceId = ankaAPI.startVM(template.getMasterVmId(), template.getTag(), startUpScript, template.getGroup(), template.getPriority(), nodeName, AnkaOnDemandSlave.getJenkinsNodeLink(nodeName), template.getVcpu(), template.getVram());
                AnkaLauncher launcher = new AnkaLauncher(this, template, newInstanceId, this.launchTimeout, this.maxLaunchRetries, this.launchRetryWaitTime, this.sshLaunchDelaySeconds, this.vmIPAssignWaitSeconds, this.vmIPAssignRetries);
                slave = new AnkaOnDemandSlave(this, nodeName, template.getDescription(), template.getRemoteFS(), template.getNumberOfExecutors(), template.getMode(), template.getLabelString(), launcher, template.getNodeProperties(), template, newInstanceId);
                newSlaves.add(slave);
            } finally {
                if (slave == null) {
                    Log("Failed to create Node " + nodeName + ". terminating " + newInstanceId);
                    terminateVMInstance(newInstanceId);
                }
            }
        }
        return newSlaves;
    }

    private boolean doesLabelMatch(final Label label, final AnkaCloudSlaveTemplate t) {
        if (t.getMode() == Node.Mode.NORMAL) {
            return (label == null || label.matches(t.getLabelSet()));
        } else if (t.getMode() == Node.Mode.EXCLUSIVE) {
            return (label != null && label.matches(t.getLabelSet()));
        }
        return false;
    }

    public AnkaCloudSlaveTemplate getTemplate(final Label label) {

        for (AnkaCloudSlaveTemplate t : this.templates) {
            if (this.doesLabelMatch(label, t)) {
                return t;
            }
        }

        for (AnkaCloudSlaveTemplate t : this.getDynamicTemplates()) {
            if (this.doesLabelMatch(label, t)) {
                return t;
            }
        }

        return null;
    }

    public boolean hasMasterVm(String templateId) {
        for (AnkaVmTemplate t : this.listVmTemplates()) {
            if (t.getId().equals(templateId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canProvision(CloudState state) {
        Label label = state.getLabel();
        AnkaCloudSlaveTemplate template = getTemplate(label);
        if (template == null) {
            return false;
        }
        int cloudCapacity = getCloudCapacity();
        if (template.getInstanceCapacity() > 0 || cloudCapacity >= 0) {
            NodeCountResponse countResponse = getNumOfRunningNodesPerLabel(label);
            if ((cloudCapacity >= 0 && countResponse.numNodes >= cloudCapacity) || (template.getInstanceCapacity() > 0 && countResponse.numNodesPerLabel >= template.getInstanceCapacity())) {
                return false;
            }
        }
        return true;
    }

    public AnkaAPI getAnkaApi() {
        return ankaAPI;
    }

    public boolean isOnline() {
        try {
            AnkaCloudStatus status = ankaAPI.getStatus();
            return status.getStatus().toLowerCase().equals("running");
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

    public void removeDynamicTemplate(AbstractSlaveTemplate template) {
        // This can be called multiple times upon termination
        this.dynamicTemplates.remove(template);  // Fails silently
    }

    public void addDynamicTemplate(DynamicSlaveTemplate template) {
        this.dynamicTemplates.add(template);
    }

    public void saveImage(AbstractAnkaSlave node) throws AnkaMgmtException {
        ImageSaver.saveImage(this, node);
    }

    public void updateInstance(String vmId, String name, String jenkinsNodeLink, String jobIdentifier) throws AnkaMgmtException {
        ankaAPI.updateInstance(vmId, name, jenkinsNodeLink, jobIdentifier);
    }

    public void terminateVMInstance(String id) throws AnkaMgmtException {
        AbstractAnkaSlave node = null;
        for (AbstractAnkaSlave ankaNode : NodeIterator.nodes(AbstractAnkaSlave.class)) {
            if (ankaNode.getInstanceId().equalsIgnoreCase(id)) {
                node = ankaNode;
            }
        }
        terminateVMInstance(id, node);
    }

    public void terminateVMInstance(String id, AbstractAnkaSlave node) throws AnkaMgmtException {
        AnkaVmInstance ankaVmInstance = ankaAPI.showInstance(id);
        ImageSaver.deleteRequest(node);
        if (ankaVmInstance == null || ankaVmInstance.isTerminatingOrTerminated()) {
            return; // if it's already terminated just forget about it
        }

        ankaAPI.terminateInstance(id);
        try {
            sleep(200);
        } catch (InterruptedException e) {
            // no rest for the wicked
        }

        while (ankaVmInstance != null && !ankaVmInstance.isTerminatingOrTerminated()) {
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

    public int getVmPollTime() {
        return this.vmPollTime;
    }

    @DataBoundSetter
    public void setVmPollTime(int milliseconds) {
        vmPollTime = milliseconds;
    }

    public Set<String> getExistingTemplateIds() {
        Set<String> l = new HashSet<String>();
        for (AnkaCloudSlaveTemplate t : this.templates) {
            String id = t.getMasterVmId();
            if (id != null) {
                l.add(id);
            }

            id = t.getTemplateId();
            if (id != null) {
                l.add(id);
            }
        }
        return l;
    }

    public Set<String> getExistingGroupIds() {
        Set<String> l = new HashSet<String>();
        for (AnkaCloudSlaveTemplate t : this.templates) {
            String id = t.getGroup();
            if (id != null) {
                l.add(id);
            }
        }
        return l;
    }

    public Set<String> getExistingTags() {
        Set<String> l = new HashSet<String>();
        for (AnkaCloudSlaveTemplate t : this.templates) {
            String id = t.getTag();
            if (id != null) {
                l.add(id);
            }

            id = t.getPushTag();
            if (id != null) {
                l.add(id);
            }
        }
        return l;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Cloud> {

        @Override
        public String getDisplayName() {
            return "Anka Build Cloud Plugin";
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context) {
            if (!(context instanceof AccessControlled ? (AccessControlled) context : Jenkins.get()).hasPermission(Computer.CONFIGURE)) {
                return new ListBoxModel();
            }

            ListBoxModel listBox = new ListBoxModel();
            listBox.add("None", "");

            CredentialsProvider.lookupCredentialsInItemGroup(
                    CertCredentials.class,
                    Jenkins.get(),
                    null,
                    null
            ).forEach(c -> listBox.add("mTLS: " + c.getName(), c.getId()));

            CredentialsProvider.lookupCredentialsInItemGroup(
                    StandardUsernamePasswordCredentials.class,
                    Jenkins.get(),
                    null,
                    null
            ).forEach(c -> {
                listBox.add("UAK/TAP: " + c.getUsername() + " (" + c.getId() + ")", c.getId());
            });

            CredentialsProvider.lookupCredentialsInItemGroup(
                    StringCredentialsImpl.class,
                    Jenkins.get(),
                    null,
                    null
            ).forEach(c -> {
                listBox.add("UAK/TAP (DEPRECATED): " + c.getId(), c.getId());
            });

            return listBox;
        }
    }
}
