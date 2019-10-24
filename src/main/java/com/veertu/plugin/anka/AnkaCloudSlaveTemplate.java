package com.veertu.plugin.anka;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.domains.SchemeRequirement;
import com.veertu.ankaMgmtSdk.AnkaVmTemplate;
import com.veertu.ankaMgmtSdk.NodeGroup;
import com.veertu.plugin.anka.exceptions.AnkaHostException;
import hudson.Extension;
import hudson.model.*;
import hudson.model.labels.LabelAtom;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.*;
import java.util.logging.Logger;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;

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
    private String cloudName;

    @DataBoundConstructor
    public AnkaCloudSlaveTemplate(
            final String cloudName) {
        this.cloudName = cloudName;
        nodeProperties = new AnkaNodeProperties();

        readResolve();
    }

    protected Object readResolve(){
        labelSet = Label.parse(nodeProperties.getLabel());
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


    public String getCloudName() {
        return cloudName;
    }

    @DataBoundSetter
    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }

    public Set<LabelAtom> getLabelSet() {
        return labelSet;
    }

    @DataBoundSetter
    public void setLabelSet(Set<LabelAtom> labelSet) {
        this.labelSet = labelSet;
    }



    public int getSchedulingTimeout() {
        if (this.schedulingTimeout <= 0)
            return DEFAULT_SCHEDULING_TIMEOUT;
        else
            return this.schedulingTimeout;
    }

    public void setNodeProperties(AnkaNodeProperties nodeProperties) {
        this.nodeProperties = nodeProperties;
    }

    public void setSaveImageParameters(SaveImageParameters saveImageParameters) {
        this.saveImageParameters = saveImageParameters;
    }

    /**
     *  ui stuff
     */


    @Override
    public Descriptor<AnkaCloudSlaveTemplate> getDescriptor() {
        return Jenkins.getInstance().getDescriptor(getClass());

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

        public ListBoxModel doFillTemplateIdItems(@QueryParameter String cloudName) {
            return doFillMasterVmIdItems(cloudName);
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
