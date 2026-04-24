package com.veertu.plugin.anka;

import hudson.PluginWrapper;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.util.VersionNumber;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Halts Jenkins startup before {@link InitMilestone#SYSTEM_CONFIG_LOADED} when the required
 * minimum versions of the Anka Build plugin's dependency plugins are not met. This runs after
 * plugins are loaded but before {@code config.xml} is deserialized, so failing fast here
 * preserves the on-disk Anka cloud configuration when an admin manually uploads a new hpi
 * onto an older Jenkins. See README.md "Dependency Requirements" and "Dependency floors" for
 * rationale.
 */
public final class DependencyVerifier {

    private static final String VERSIONS_RESOURCE =
            "/com/veertu/plugin/anka/required-dep-versions.properties";

    private DependencyVerifier() {}

    /**
     * System property to bypass the verifier. Used by the Jenkins test harness where optional
     * dependency plugins are intentionally absent, and as an emergency admin override.
     */
    static final String SKIP_PROPERTY = "com.veertu.plugin.anka.DependencyVerifier.skip";

    @Initializer(before = InitMilestone.SYSTEM_CONFIG_LOADED, fatal = true)
    public static void verifyDependencies() {
        if (hudson.Main.isUnitTest || Boolean.getBoolean(SKIP_PROPERTY)) {
            return;
        }
        Map<String, String> requiredMinVersions = loadRequiredMinVersions();
        List<String> failures = checkDependencies(requiredMinVersions, shortName -> {
            PluginWrapper installed = Jenkins.get().getPluginManager().getPlugin(shortName);
            if (installed == null) {
                return null;
            }
            return new InstalledPlugin(installed.getVersion(), installed.isActive());
        });

        if (!failures.isEmpty()) {
            String msg = "[anka-build] Halting Jenkins startup to prevent config.xml data loss. "
                    + "The Anka Build plugin requires the following dependency plugins at "
                    + "minimum versions that are NOT met on this Jenkins instance:\n"
                    + String.join("\n", failures)
                    + "\n\n"
                    + "Please upgrade the dependencies above and restart Jenkins. "
                    + "Your Anka cloud configuration in config.xml has NOT been modified.";
            throw new Error(msg);
        }
    }

    /**
     * Pure check logic, factored out so it can be unit-tested without booting Jenkins. The
     * {@code pluginLookup} returns {@link InstalledPlugin} descriptors (or {@code null} when the
     * plugin is not installed) so tests don't need to construct real {@link PluginWrapper}s.
     */
    static List<String> checkDependencies(Map<String, String> requiredMinVersions,
                                          PluginLookup pluginLookup) {
        List<String> failures = new ArrayList<>();
        for (Map.Entry<String, String> entry : requiredMinVersions.entrySet()) {
            String shortName = entry.getKey();
            VersionNumber required = new VersionNumber(entry.getValue());
            InstalledPlugin installed = pluginLookup.getPlugin(shortName);

            if (installed == null) {
                failures.add(String.format("  - %s is not installed (required >= %s)",
                        shortName, required));
                continue;
            }
            if (!installed.active) {
                failures.add(String.format("  - %s is installed but disabled (required >= %s)",
                        shortName, required));
                continue;
            }
            VersionNumber actual = new VersionNumber(installed.version);
            if (actual.isOlderThan(required)) {
                failures.add(String.format("  - %s is %s (required >= %s)",
                        shortName, actual, required));
            }
        }
        return failures;
    }

    static Map<String, String> loadRequiredMinVersions() {
        Properties props = new Properties();
        try (InputStream in = DependencyVerifier.class.getResourceAsStream(VERSIONS_RESOURCE)) {
            if (in == null) {
                throw new Error("[anka-build] " + VERSIONS_RESOURCE
                        + " not found on classpath; plugin build is broken.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new Error("[anka-build] Failed to load " + VERSIONS_RESOURCE, e);
        }
        Map<String, String> out = new TreeMap<>();
        for (String name : props.stringPropertyNames()) {
            out.put(name, props.getProperty(name).trim());
        }
        return out;
    }

    @FunctionalInterface
    interface PluginLookup {
        InstalledPlugin getPlugin(String shortName);
    }

    /** Minimal view of an installed Jenkins plugin used by {@link #checkDependencies}. */
    static final class InstalledPlugin {
        final String version;
        final boolean active;

        InstalledPlugin(String version, boolean active) {
            this.version = version;
            this.active = active;
        }
    }
}
