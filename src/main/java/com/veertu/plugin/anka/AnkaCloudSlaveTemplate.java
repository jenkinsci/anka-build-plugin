package com.veertu.plugin.anka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.domains.SchemeRequirement;
import com.veertu.ankaMgmtSdk.AnkaVmTemplate;
import com.veertu.ankaMgmtSdk.NodeGroup;
import com.veertu.plugin.anka.exceptions.AnkaHostException;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Computer;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.ItemGroup;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

/**
 * Created by avia on 10/07/2016.
 */
public class AnkaCloudSlaveTemplate extends AbstractSlaveTemplate implements Describable<AnkaCloudSlaveTemplate> {

    public static final String BridgedNetwork = "bridge";
    public static String SharedNetwork = "shared";
    public static String HostNetwork = "host";
    private static final transient Logger LOGGER = Logger.getLogger(AnkaCloudSlaveTemplate.class.getName());
    private int schedulingTimeout = DEFAULT_SCHEDULING_TIMEOUT;
    private Set<LabelAtom> labelSet;

    @DataBoundConstructor
    public AnkaCloudSlaveTemplate(
            final String cloudName, final String remoteFS, final String masterVmId,
            final String tag, final String label, final String templateDescription,
            final int numberOfExecutors, final int launchDelay,
            boolean keepAliveOnError,
            String launchMethod,
            String group,
            String nameTemplate, int priority, int schedulingTimeout,
            int vcpu, int vram,
            @Nullable Boolean saveImage, @Nullable String templateId,
            @Nullable String pushTag,
            @Nullable Boolean dontAppendTimestamp,
            @Nullable Boolean deleteLatest,
            @Nullable String description, @Nullable Boolean suspend, @Nullable Boolean waitForBuildToFinish,
            @Nullable List<EnvironmentEntry> environments) {
        saveImageParameters = new SaveImageParameters();
        this.cloudName = cloudName;
        setRemoteFS(remoteFS);
        setMasterVmId(masterVmId);
        setTag(tag);
        setLabel(label);
        setVcpu(vcpu);
        setVram(vram);
        setTemplateDescription(templateDescription);
        setNumberOfExecutors(numberOfExecutors);
        setLaunchDelay(launchDelay);
        setKeepAliveOnError(keepAliveOnError);
        setLaunchMethod(launchMethod);
        setGroup(group);
        setNameTemplate(nameTemplate);
        setPriority(priority);
        this.schedulingTimeout = schedulingTimeout;

        setSaveImage(saveImage);
        setTemplateId(templateId);
        setPushTag(pushTag);
        setDontAppendTimestamp(dontAppendTimestamp);
        setDeleteLatest(deleteLatest);
        setDescription(description);
        setSuspend(suspend);
        setWaitForBuildToFinish(waitForBuildToFinish);
        setEnvironments(environments);
        setMode(Node.Mode.EXCLUSIVE);
        readResolve();
    }

    public AnkaCloudSlaveTemplate() {
        saveImageParameters = new SaveImageParameters();
        readResolve();
    }

    protected Object readResolve(){
        labelSet = Label.parse(getLabel());
        return this;
    }

    public static Logger getLOGGER() {
        return LOGGER;
    }

    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();
        if (cloudName != null && labelString != null) {
            sb.append(cloudName).append(" ").append(labelString);
            return sb.toString();
        }

        if (cloudName != null) {
            sb.append(cloudName);
        }

        if (labelString != null) {
            sb.append(labelString);
        }

        return sb.toString();
    }

    public static SchemeRequirement getHTTP_SCHEME() {
        return HTTP_SCHEME;
    }

    public static SchemeRequirement getHTTPS_SCHEME() {
        return HTTPS_SCHEME;
    }

    public Set<LabelAtom> getLabelSet() {
        return labelSet;
    }

    @DataBoundSetter
    public void setLabelSet(Set<LabelAtom> labelSet) {
        this.labelSet = labelSet;
    }

    @Deprecated
    @DataBoundSetter
    public void setLaunchMethodString(String method) {
        setLaunchMethod(method);
    }

    @Deprecated
    public String getLaunchMethodString() {
        return getLaunchMethod();
    }

    public int getSchedulingTimeout() {
        if (this.schedulingTimeout <= 0)
            return DEFAULT_SCHEDULING_TIMEOUT;
        else
            return this.schedulingTimeout;
    }

    public void setSchedulingTimeout(int timeout) {
        if (timeout > 0) {
            this.schedulingTimeout = timeout;
        }
    }

    public void setSaveImageParameters(SaveImageParameters saveImageParameters) {
        this.saveImageParameters = saveImageParameters;
    }

    /**
     *  ui stuff
     */


    @Override
    public Descriptor<AnkaCloudSlaveTemplate> getDescriptor() {
        return Jenkins.get().getDescriptor(getClass());

    }

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
            System.out.println("configure");
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
            if (!(context instanceof AccessControlled ? (AccessControlled) context : Jenkins.get()).hasPermission(Computer.CONFIGURE)) {
                return new ListBoxModel();
            }
            final List<StandardUsernameCredentials> credentials = lookupCredentials(StandardUsernameCredentials.class, context, ACL.SYSTEM, HTTP_SCHEME, HTTPS_SCHEME);
            StandardUsernameListBoxModel listBox = new StandardUsernameListBoxModel();
            for (StandardUsernameCredentials cred: credentials) {
                listBox.with(cred);
            }
            return listBox;
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
            AnkaMgmtCloud cloud = (AnkaMgmtCloud) Jenkins.get().getCloud(cloudName);
            ListBoxModel models = new ListBoxModel();
            models.add("Choose Node Group Or Leave Empty for all", "");
            if (!cloud.isOnline()) {
                List<String> groupIds = new ArrayList<>(cloud.getExistingGroupIds());
                groupIds.sort(String::compareToIgnoreCase);
                for (String groupId : groupIds) {
                    models.add(groupId);
                }
            } else {
                List<NodeGroup> nodeGroups = new ArrayList<>(getNodeGroups(cloud));
                nodeGroups.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                for (NodeGroup nodeGroup : nodeGroups) {
                    models.add(nodeGroup.getName(), nodeGroup.getId());
                }
            }
            return models;
        }

        public ListBoxModel doFillMasterVmIdItems(@QueryParameter String cloudName) {
            AnkaMgmtCloud cloud = (AnkaMgmtCloud) Jenkins.get().getCloud(cloudName);
            // java.util.logging.Logger logger = java.util.logging.Logger.getLogger("jenkins.system.log");
            // logger.info("cloudName: " + cloudName);
            // logger.info("cloud: " + cloud);
            ListBoxModel models = new ListBoxModel();
            models.add("Choose Vm template", "");
            if (! cloud.isOnline()) {
                if (! cloud.isOnline()) {
                    for (String id: cloud.getExistingTemplateIds()) {
                        models.add(id);
                    }
                }
            }
            else {
                if (cloud != null) {
                    for (AnkaVmTemplate temp: cloud.listVmTemplates()){
                        models.add(String.format("%s(%s)", temp.getName(), temp.getId()), temp.getId());
                    }
                }
            }
            return models;
        }

        public ListBoxModel doFillTemplateIdItems(@QueryParameter String cloudName) {
            return doFillMasterVmIdItems(cloudName);
        }

        public ListBoxModel doFillTagItems(@QueryParameter String cloudName , @QueryParameter String masterVmId) {
            AnkaMgmtCloud cloud = (AnkaMgmtCloud) Jenkins.get().getCloud(cloudName);
            ListBoxModel models = new ListBoxModel();
            models.add("Choose a Tag or leave empty for latest", "");
            if (! cloud.isOnline()) {
                for (String tagName: cloud.getExistingTags()){
                    models.add(tagName);
                }
            }
            else {
                if (cloud != null && masterVmId != null && masterVmId.length() != 0) {
                    for (String tagName: cloud.getTemplateTags(masterVmId)){
                        models.add(tagName, tagName);
                    }
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
