package com.veertu.plugin.anka;

import hudson.Util;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * Parses JSON, merges templates, and persists {@link AnkaMgmtCloud} static labels.
 */
final class AnkaLabelsTemplateService {

    private AnkaLabelsTemplateService() {}

    static Result apply(AnkaMgmtCloud cloud, String jsonBody) throws IOException {
        if (jsonBody == null || jsonBody.isBlank()) {
            throw new IllegalArgumentException("Request body is required");
        }
        final JSONObject root;
        try {
            root = JSONObject.fromObject(jsonBody);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid JSON: " + e.getMessage());
        }
        String mode = root.optString("mode", "").trim().toLowerCase(Locale.ROOT);
        if (!"append".equals(mode) && !"replace".equals(mode)) {
            throw new IllegalArgumentException("mode must be 'append' or 'replace'");
        }
        JSONArray templatesArray = root.optJSONArray("templates");
        if (templatesArray == null) {
            throw new IllegalArgumentException("templates array is required");
        }
        List<AnkaCloudSlaveTemplate> incoming = parseTemplates(cloud, templatesArray);
        validateNoDuplicateLabels(incoming);

        int previousCount = cloud.getTemplates().size();
        List<AnkaCloudSlaveTemplate> merged;
        if ("replace".equals(mode)) {
            merged = new ArrayList<>(incoming);
        } else {
            merged = mergeAppend(cloud.getTemplates(), incoming);
        }

        for (AnkaCloudSlaveTemplate t : merged) {
            if (cloud.isOnline() && !cloud.hasMasterVm(t.getMasterVmId())) {
                AnkaMgmtCloud.Log(
                        "Labels API: masterVmId '%s' for label '%s' not found on controller (cloud offline or id typo?)",
                        t.getMasterVmId(),
                        t.getLabelString());
            }
        }

        AnkaMgmtCloud replacement = cloud.copyWithTemplates(merged);
        cloud.replaceInJenkinsWith(replacement);
        return new Result(cloud.getCloudName(), mode, previousCount, merged.size());
    }

    private static List<AnkaCloudSlaveTemplate> parseTemplates(AnkaMgmtCloud cloud, JSONArray templatesArray) {
        StaplerRequest2 req = Stapler.getCurrentRequest2();
        if (req == null) {
            throw new IllegalStateException("No Stapler request in context");
        }
        List<AnkaCloudSlaveTemplate> out = new ArrayList<>();
        for (int i = 0; i < templatesArray.size(); i++) {
            Object el = templatesArray.get(i);
            JSONObject jo = el instanceof JSONObject ? (JSONObject) el : JSONObject.fromObject(el);
            validateTemplateJsonTypes(jo, i);
            AnkaCloudSlaveTemplate t;
            try {
                t = req.bindJSON(AnkaCloudSlaveTemplate.class, jo);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "templates[" + i + "]: could not parse as AnkaCloudSlaveTemplate: " + e.getMessage());
            }
            String label = Util.fixEmptyAndTrim(t.getLabelString());
            String masterVmId = Util.fixEmptyAndTrim(t.getMasterVmId());
            if (label == null) {
                throw new IllegalArgumentException("templates[" + i + "]: label is required");
            }
            if (masterVmId == null) {
                throw new IllegalArgumentException("templates[" + i + "]: masterVmId is required");
            }
            if (Util.fixEmptyAndTrim(t.getCloudName()) == null) {
                t.setCloudName(cloud.getCloudName());
            }
            out.add(t);
        }
        return out;
    }

    /**
     * Stapler {@code bindJSON} coerces mismatched types (e.g. string to int); reject invalid JSON types up front.
     */
    private static void validateTemplateJsonTypes(JSONObject jo, int index) {
        String prefix = "templates[" + index + "]";
        requireJsonNumberIfPresent(jo, prefix, "numberOfExecutors");
        requireJsonNumberIfPresent(jo, prefix, "launchDelay");
        requireJsonNumberIfPresent(jo, prefix, "priority");
        requireJsonNumberIfPresent(jo, prefix, "schedulingTimeout");
        requireJsonNumberIfPresent(jo, prefix, "vcpu");
        requireJsonNumberIfPresent(jo, prefix, "vram");
        requireJsonNumberIfPresent(jo, prefix, "SSHPort");
        requireJsonNumberIfPresent(jo, prefix, "idleMinutes");
        requireJsonNumberIfPresent(jo, prefix, "instanceCapacity");
        requireJsonBooleanIfPresent(jo, prefix, "keepAliveOnError");
        requireJsonBooleanIfPresent(jo, prefix, "saveImage");
        requireJsonBooleanIfPresent(jo, prefix, "dontAppendTimestamp");
        requireJsonBooleanIfPresent(jo, prefix, "deleteLatest");
        requireJsonBooleanIfPresent(jo, prefix, "suspend");
        requireJsonBooleanIfPresent(jo, prefix, "waitForBuildToFinish");
        if (jo.has("saveImageParameters") && jo.get("saveImageParameters") instanceof JSONObject saveImageParameters) {
            String nested = prefix + ".saveImageParameters";
            requireJsonBooleanIfPresent(saveImageParameters, nested, "saveImage");
            requireJsonBooleanIfPresent(saveImageParameters, nested, "dontAppendTimestamp");
            requireJsonBooleanIfPresent(saveImageParameters, nested, "deleteLatest");
            requireJsonBooleanIfPresent(saveImageParameters, nested, "suspend");
            requireJsonBooleanIfPresent(saveImageParameters, nested, "waitForBuildToFinish");
        }
    }

    private static void requireJsonNumberIfPresent(JSONObject jo, String prefix, String field) {
        if (!jo.has(field)) {
            return;
        }
        Object value = jo.get(field);
        if (value == null || value == JSONNull.getInstance()) {
            return;
        }
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException(prefix + ": " + field + " must be a JSON number");
        }
    }

    private static void requireJsonBooleanIfPresent(JSONObject jo, String prefix, String field) {
        if (!jo.has(field)) {
            return;
        }
        Object value = jo.get(field);
        if (value == null || value == JSONNull.getInstance()) {
            return;
        }
        if (!(value instanceof Boolean)) {
            throw new IllegalArgumentException(prefix + ": " + field + " must be a JSON boolean");
        }
    }

    private static void validateNoDuplicateLabels(List<AnkaCloudSlaveTemplate> incoming) {
        Set<String> seen = new LinkedHashSet<>();
        for (AnkaCloudSlaveTemplate t : incoming) {
            String label = t.getLabelString();
            if (!seen.add(label)) {
                throw new IllegalArgumentException("Duplicate label in request: " + label);
            }
        }
    }

    private static List<AnkaCloudSlaveTemplate> mergeAppend(
            List<AnkaCloudSlaveTemplate> existing, List<AnkaCloudSlaveTemplate> incoming) {
        List<AnkaCloudSlaveTemplate> merged = new ArrayList<>(existing);
        for (AnkaCloudSlaveTemplate in : incoming) {
            int idx = -1;
            for (int i = 0; i < merged.size(); i++) {
                if (merged.get(i).getLabelString().equals(in.getLabelString())) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) {
                merged.set(idx, in);
            } else {
                merged.add(in);
            }
        }
        return merged;
    }

    static final class Result {
        final String cloudName;
        final String mode;
        final int previousCount;
        final int newCount;

        Result(String cloudName, String mode, int previousCount, int newCount) {
            this.cloudName = cloudName;
            this.mode = mode;
            this.previousCount = previousCount;
            this.newCount = newCount;
        }

        String toJson() {
            JSONObject o = new JSONObject();
            o.put("cloudName", cloudName);
            o.put("mode", mode);
            o.put("previousCount", previousCount);
            o.put("newCount", newCount);
            return o.toString();
        }
    }
}
