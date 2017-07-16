package com.veertuci.plugins.anka.parameters;

import com.veertuci.plugins.AnkaMgmtCloud;
import hudson.model.ParameterValue;
import hudson.Extension;
import hudson.model.SimpleParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.slaves.Cloud;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by avia on 26/07/2016.
 */
public class CloudSelectorParameter extends SimpleParameterDefinition {


    @DataBoundConstructor
    public CloudSelectorParameter() {
        super("VEERTU_CLOUD_NAME", "Veertu Cloud Selector");
    }

    @Override
    public StringParameterValue getDefaultParameterValue() {
        List<String> cloudNames = getAnkaCloudNames();
        return new StringParameterValue(getName(), cloudNames.get(0), getDescription());
    }

    private StringParameterValue checkValue(StringParameterValue value) {
        List<String> cloudNames = getAnkaCloudNames();
        if (!cloudNames.contains(value.value))
            throw new IllegalArgumentException("No anka cloud with name: " + value.value);
        return value;
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        StringParameterValue value = req.bindJSON(StringParameterValue.class, jo);
        value.setDescription(getDescription());
        return checkValue(value);
    }

    public StringParameterValue createValue(String value) {
        return checkValue(new StringParameterValue(getName(), value, getDescription()));
    }

    @Exported
    public List<String> getCloudNames() {
        return getAnkaCloudNames();
    }

    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {

        public ListBoxModel doFillCloudNameItems() {
            ListBoxModel items = new ListBoxModel();
            for (String cloudName : getAnkaCloudNames()) {
                items.add(cloudName);
            }
            return items;
        }

        @Override
        public String getDisplayName() {
            return "Veertu Cloud Selection Parameter";
        }
    }

    public static List<AnkaMgmtCloud> getAnkaClouds() {
        List<AnkaMgmtCloud> ankaClouds = new ArrayList<AnkaMgmtCloud>();
        for (Cloud cloud : Jenkins.getInstance().clouds) {
            if (cloud instanceof AnkaMgmtCloud) {
                ankaClouds.add((AnkaMgmtCloud) cloud);
            }
        }
        return ankaClouds;
    }

    public static List<String> getAnkaCloudNames() {
        List<String> cloudNames = new ArrayList<>();
        for (AnkaMgmtCloud ankaCloud : getAnkaClouds()) {
            cloudNames.add(ankaCloud.getCloudName());
        }
        return cloudNames;
    }
}
