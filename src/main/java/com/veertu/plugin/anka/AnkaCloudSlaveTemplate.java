package com.veertu.plugin.anka;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.domains.SchemeRequirement;
import com.veertu.ankaMgmtSdk.AnkaVmTemplate;
import com.veertu.ankaMgmtSdk.NodeGroup;
import com.veertu.plugin.anka.exceptions.AnkaHostException;
import hudson.Extension;
import hudson.model.*;
import hudson.model.Node.Mode;
import hudson.model.labels.LabelAtom;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import hudson.slaves.RetentionStrategy;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.DataBoundSetter;
import javax.annotation.Nullable;

import java.util.*;
import java.util.logging.Logger;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;

/**
 * Created by avia on 10/07/2016.
 */
public class AnkaCloudSlaveTemplate implements Describable<AnkaCloudSlaveTemplate> {

    protected static final SchemeRequirement HTTP_SCHEME = new SchemeRequirement("http");
    protected static final SchemeRequirement HTTPS_SCHEME = new SchemeRequirement("https");
    protected static final int DEFAULT_SCHEDULING_TIMEOUT = 180;
    public static final String BridgedNetwork = "bridge";
    public static String SharedNetwork = "shared";
    public static String HostNetwork = "host";
    private static final transient Logger LOGGER = Logger.getLogger(AnkaCloudSlaveTemplate.class.getName());
    //private final List<String> masterImages;
    private final String masterVmId;
    private final String tag;
    private final int launchDelay;
    private final String remoteFS;
    private final String labelString;
    private final String templateDescription;
    private final int numberOfExecutors;
    private final Mode mode;
    private final String credentialsId;
    private final String group;

    private final String extraArgs;

    private LaunchMethod launchMethod = LaunchMethod.SSH;
    private String launchMethodString = "ssh";
    //    private final List<? extends NodeProperty<?>> nodeProperties;
    private transient Set<LabelAtom> labelSet;
    private final boolean keepAliveOnError;
    private final int SSHPort;
    private final String cloudName;
    private List<EnvironmentEntry> environments;
    private RetentionStrategy retentionStrategy = new RunOnceCloudRetentionStrategy(1);
    private final String nameTemplate;
    private String javaArgs;
    private String jnlpJenkinsOverrideUrl;
    private String jnlpTunnel;
    private int priority;
    private int schedulingTimeout = DEFAULT_SCHEDULING_TIMEOUT;

    @DataBoundConstructor
    public AnkaCloudSlaveTemplate(
            final String cloudName, final String remoteFS, final String masterVmId,
            final String tag, final String labelString, final String templateDescription,
            final int numberOfExecutors, final int launchDelay,
            boolean keepAliveOnError, JSONObject launchMethod, String group, String nameTemplate, int priority, int schedulingTimeout, @Nullable List<EnvironmentEntry> environments) {
        this.remoteFS = remoteFS;
        this.labelString = labelString;
        this.templateDescription = templateDescription;
        this.numberOfExecutors = numberOfExecutors;
        this.masterVmId = masterVmId;
        this.tag = tag;
        // this.selectedMasterImage=selectedMasterImage;
        this.mode = Mode.EXCLUSIVE;
        this.credentialsId = launchMethod.optString("credentialsId", null);
        this.launchDelay = launchDelay;
        this.keepAliveOnError = keepAliveOnError;
        this.SSHPort = 22;
        this.cloudName = cloudName;
        this.environments = environments;
        this.nameTemplate = nameTemplate;
        this.group = group;
        this.extraArgs = launchMethod.optString("extraArgs", null);
        this.javaArgs = launchMethod.optString("javaArgs", null);
        this.jnlpJenkinsOverrideUrl = launchMethod.optString("jnlpJenkinsOverrideUrl", null);
        this.jnlpTunnel = launchMethod.optString("jnlpTunnel", null);
        this.setLaunchMethod(launchMethod.getString("value"));
        this.priority = priority;
        if (schedulingTimeout <= 0)
            this.schedulingTimeout = DEFAULT_SCHEDULING_TIMEOUT;
        else
            this.schedulingTimeout = schedulingTimeout;
        readResolve();
    }

    protected Object readResolve() {
        this.labelSet = Label.parse(labelString);

        return this;
    }

    public static Logger getLOGGER() {
        return LOGGER;
    }

    public static SchemeRequirement getHTTP_SCHEME() {
        return HTTP_SCHEME;
    }

    public static SchemeRequirement getHTTPS_SCHEME() {
        return HTTPS_SCHEME;
    }


    public boolean isKeepAliveOnError() {
        return keepAliveOnError;
    }


    public String getMasterVmId() {
        return masterVmId;
    }

    public String getNameTemplate() {
        return nameTemplate;
    }

    public String getTag() {
        return tag;
    }

    public int getLaunchDelay() {
        return launchDelay;
    }

    public String getRemoteFS() {

        return remoteFS;
    }

    public String getLabelString() {
        return labelString;
    }

    public String getTemplateDescription() {
        return templateDescription;
    }

    public int getNumberOfExecutors() {
        return numberOfExecutors;
    }

    public Mode getMode() {
        return mode;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public Set<LabelAtom> getLabelSet() {
        return this.labelSet;
    }


    public String getGroup() {
        return group;
    }


    @Override
    public Descriptor<AnkaCloudSlaveTemplate> getDescriptor() {
        return Jenkins.getInstance().getDescriptor(getClass());

    }

    public boolean isNetworkBridged() {
        return false;
    }

    public int getSSHPort() {
        return SSHPort;
    }

    public String getCloudName() {
        return cloudName;
    }

    public RetentionStrategy getRetentionStrategy() { return retentionStrategy; }

    public List<EnvironmentEntry> getEnvironments()
    {
        if (environments != null)
            return environments;
        return new ArrayList<EnvironmentEntry>();
    }

    public String getJnlpArgsString() {
        return extraArgs;
    }

    public String getExtraArgs() {
        return getJnlpArgsString();
    }

    /*Collection<KeyValuePair> getEnvironmentKeyValuePairs() {
        if (null == environments || environments.isEmpty()) {
            return null;
        }
        Collection<KeyValuePair> items = new ArrayList<KeyValuePair>();
        for (EnvironmentEntry environment : environments) {
            String name = environment.name;
            String value = environment.value;
            if (StringUtils.isEmpty(name) || StringUtils.isEmpty(value)) {
                continue;
            }
            items.add(new KeyValuePair().withName(name).withValue(value));
        }
        return items;
    }*/


    @DataBoundSetter
    public void setRetentionStrategy(RetentionStrategy retentionStrategy) {
        this.retentionStrategy = retentionStrategy;
    }

    public void setLaunchMethod(String launchMethod) {
        this.launchMethodString = launchMethod;
        if (launchMethod.equals("ssh")) {
            this.launchMethod = LaunchMethod.SSH;
        } else {
            this.launchMethod = LaunchMethod.JNLP;
        }
    }

    public String getLaunchMethodString() {
        return launchMethodString;
    }

    public LaunchMethod getLaunchMethod() {
        return this.launchMethod;
    }

    public String getJavaArgs() {
        return this.javaArgs;
    }

    public String getJnlpJenkinsOverrideUrl() {
        return jnlpJenkinsOverrideUrl;
    }
    public String getJnlpTunnel() {
        if (jnlpTunnel == null)
            return "";
        return jnlpTunnel;
    }

    public int getPriority() {
        return this.priority;
    }

    public int getSchedulingTimeout() {
        if (this.schedulingTimeout <= 0)
            return DEFAULT_SCHEDULING_TIMEOUT;
        else
            return this.schedulingTimeout;
    }

    /**
     *  ui stuff
     */

    public static class EnvironmentEntry extends AbstractDescribableImpl<EnvironmentEntry> {
        public String name, value;

        @DataBoundConstructor
        public EnvironmentEntry(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return "EnvironmentEntry{" + name + ": " + value + "}";
        }

        @Extension
        public static class DescriptorImpl extends Descriptor<EnvironmentEntry> {
            @Override
            public String getDisplayName() {
                return "EnvironmentEntry";
            }
        }
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<AnkaCloudSlaveTemplate> {

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return super.configure(req, formData);
            //return true;
        }

        @Override
        public String getDisplayName() {
            return null;
        }
        

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context) {
            if (!(context instanceof AccessControlled ? (AccessControlled) context : Jenkins.getInstance()).hasPermission(Computer.CONFIGURE)) {
                return new ListBoxModel();
            }
            final List<StandardUsernameCredentials> credentials = lookupCredentials(StandardUsernameCredentials.class, context, ACL.SYSTEM, HTTP_SCHEME, HTTPS_SCHEME);
            return new StandardUsernameListBoxModel().withAll(credentials);
        }

        public List<AnkaVmTemplate> getClonableVms(AnkaMgmtCloud cloud) throws AnkaHostException {
            try {
                if (cloud != null) {
                    return cloud.listVmTemplates();
                }
                return new ArrayList<>();
            }
            catch (Exception e) {
                return new ArrayList<>();
            }
        }

        public List<String> getTemplateTags(AnkaMgmtCloud cloud, String masterVmId) throws AnkaHostException {
            if (cloud != null && masterVmId != null && masterVmId.length() != 0) {
                return cloud.getTemplateTags(masterVmId);
            }
            return new ArrayList<>();
        }

        public List<NodeGroup> getNodeGroups(AnkaMgmtCloud cloud) {
            if (cloud != null) {
                return cloud.getNodeGroups();
            }
            return new ArrayList<>();
        }

        public ListBoxModel doFillGroupItems(@QueryParameter String cloudName) {
            AnkaMgmtCloud cloud = (AnkaMgmtCloud) Jenkins.getInstance().getCloud(cloudName);
            ListBoxModel models = new ListBoxModel();
            models.add("Choose Node Group Or Leave Empty for all", null);
            for (NodeGroup nodeGroup: getNodeGroups(cloud)) {
                models.add(nodeGroup.getName(), nodeGroup.getId());
            }
            return models;
        }

        public ListBoxModel doFillMasterVmIdItems(@QueryParameter String cloudName) {
            AnkaMgmtCloud cloud = (AnkaMgmtCloud) Jenkins.getInstance().getCloud(cloudName);
            ListBoxModel models = new ListBoxModel();
            models.add("Choose Vm template", null);
            if (cloud != null) {
                for (AnkaVmTemplate temp: cloud.listVmTemplates()){
                    models.add(String.format("%s(%s)", temp.getName(), temp.getId()), temp.getId());
                }
            }
            return models;
        }

        public ListBoxModel doFillTagItems(@QueryParameter String cloudName , @QueryParameter String masterVmId) {
            AnkaMgmtCloud cloud = (AnkaMgmtCloud) Jenkins.getInstance().getCloud(cloudName);
            ListBoxModel models = new ListBoxModel();
            models.add("Choose a Tag or leave empty for latest", null);
            if (cloud != null && masterVmId != null && masterVmId.length() != 0) {
                for (String tagName: cloud.getTemplateTags(masterVmId)){
                    models.add(tagName, tagName);
                }
            }
            return models;
        }

        public List<String> getNetworkConfigOptions(){
            return Arrays.asList(HostNetwork, SharedNetwork);
        }

        public int getSchedulingTimeout() {
            return DEFAULT_SCHEDULING_TIMEOUT;
        }

    }


}
