package com.veertu.plugin.anka;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

import com.veertu.plugin.anka.DependencyVerifier.InstalledPlugin;
import com.veertu.plugin.anka.DependencyVerifier.PluginLookup;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class DependencyVerifierTest {

    private static final String REQUIRED_VERSION = "199.v9f8e1f741799";
    private static final String NEWER_VERSION = "200.v00000000001";
    private static final String OLDER_VERSION = "183.va_de8f1dd5a_2b_";

    private static Map<String, String> singleRequired(String shortName) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(shortName, REQUIRED_VERSION);
        return map;
    }

    private static PluginLookup lookupOf(Map<String, InstalledPlugin> installed) {
        return installed::get;
    }

    @Test
    public void missingPlugin_reportedAsNotInstalled() {
        List<String> failures = DependencyVerifier.checkDependencies(
                singleRequired("plain-credentials"), lookupOf(new HashMap<>()));

        assertThat(failures, hasSize(1));
        assertThat(failures.get(0), containsString("plain-credentials"));
        assertThat(failures.get(0), containsString("not installed"));
        assertThat(failures.get(0), containsString(REQUIRED_VERSION));
    }

    @Test
    public void disabledPlugin_reportedAsDisabled() {
        Map<String, InstalledPlugin> installed = new HashMap<>();
        installed.put("plain-credentials", new InstalledPlugin(NEWER_VERSION, false));

        List<String> failures = DependencyVerifier.checkDependencies(
                singleRequired("plain-credentials"), lookupOf(installed));

        assertThat(failures, hasSize(1));
        assertThat(failures.get(0), containsString("plain-credentials"));
        assertThat(failures.get(0), containsString("installed but disabled"));
    }

    @Test
    public void tooOldPlugin_reportedWithActualAndRequiredVersions() {
        Map<String, InstalledPlugin> installed = new HashMap<>();
        installed.put("plain-credentials", new InstalledPlugin(OLDER_VERSION, true));

        List<String> failures = DependencyVerifier.checkDependencies(
                singleRequired("plain-credentials"), lookupOf(installed));

        assertThat(failures, hasSize(1));
        assertThat(failures.get(0), containsString("plain-credentials"));
        assertThat(failures.get(0), containsString(OLDER_VERSION));
        assertThat(failures.get(0), containsString(REQUIRED_VERSION));
    }

    @Test
    public void exactMatchVersion_noFailure() {
        Map<String, InstalledPlugin> installed = new HashMap<>();
        installed.put("plain-credentials", new InstalledPlugin(REQUIRED_VERSION, true));

        List<String> failures = DependencyVerifier.checkDependencies(
                singleRequired("plain-credentials"), lookupOf(installed));

        assertThat(failures, empty());
    }

    @Test
    public void newerVersion_noFailure() {
        Map<String, InstalledPlugin> installed = new HashMap<>();
        installed.put("plain-credentials", new InstalledPlugin(NEWER_VERSION, true));

        List<String> failures = DependencyVerifier.checkDependencies(
                singleRequired("plain-credentials"), lookupOf(installed));

        assertThat(failures, empty());
    }

    @Test
    public void multipleFailures_allReported() {
        Map<String, String> required = new LinkedHashMap<>();
        required.put("plugin-missing", REQUIRED_VERSION);
        required.put("plugin-disabled", REQUIRED_VERSION);
        required.put("plugin-too-old", REQUIRED_VERSION);
        required.put("plugin-ok", REQUIRED_VERSION);

        Map<String, InstalledPlugin> installed = new HashMap<>();
        installed.put("plugin-disabled", new InstalledPlugin(NEWER_VERSION, false));
        installed.put("plugin-too-old", new InstalledPlugin(OLDER_VERSION, true));
        installed.put("plugin-ok", new InstalledPlugin(REQUIRED_VERSION, true));

        List<String> failures = DependencyVerifier.checkDependencies(required, lookupOf(installed));

        assertThat(failures, hasSize(3));
        assertThat(failures, contains(
                containsString("plugin-missing"),
                containsString("plugin-disabled"),
                containsString("plugin-too-old")));
    }

    @Test
    public void loadRequiredMinVersions_readsFilteredPropertiesResource() {
        Map<String, String> versions = DependencyVerifier.loadRequiredMinVersions();

        assertThat("filtered properties file should expose all Jenkins plugin deps",
                versions.keySet(), contains(
                        "bouncycastle-api",
                        "node-iterator-api",
                        "plain-credentials",
                        "ssh-slaves",
                        "workflow-basic-steps",
                        "workflow-durable-task-step"));
        for (Map.Entry<String, String> entry : versions.entrySet()) {
            String value = entry.getValue();
            assertThat("required version for " + entry.getKey() + " must be filtered, not a ${...} placeholder",
                    value, org.hamcrest.Matchers.not(containsString("${")));
        }
    }
}
