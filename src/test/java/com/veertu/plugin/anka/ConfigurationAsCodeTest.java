package com.veertu.plugin.anka;

import hudson.model.Node.Mode;
import hudson.slaves.RetentionStrategy;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import java.util.List;

import static io.jenkins.plugins.casc.misc.Util.getJenkinsRoot;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

public class ConfigurationAsCodeTest {

    @Rule
    public JenkinsConfiguredWithCodeRule r = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    public void should_support_configuration_as_code() {
        validateCasCLoading((AnkaMgmtCloud) r.jenkins.clouds.get(0));
    }

    @Test
    @Issue("JENKINS-69035")
    @ConfiguredWithCode("configuration-as-code-legacy.yml")
    public void should_support_legacy_configuration_as_code() {
        validateCasCLoading((AnkaMgmtCloud) r.jenkins.clouds.get(0));
    }

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    public void should_support_configuration_export() throws Exception {
        validateCasCExport();
    }

    @Test
    @Issue("JENKINS-69035")
    @ConfiguredWithCode("configuration-as-code-legacy.yml")
    public void should_support_legacy_configuration_export() throws Exception {
        validateCasCExport();
    }
    
    private void validateCasCLoading(AnkaMgmtCloud cloud) {
        assertThat(cloud.getCloudName(), is("Veertu anka"));
        assertThat(cloud.getAnkaMgmtUrl(), is("https://veertu-anka"));
        assertThat(cloud.getCloudCapacity(), is(100));
        assertThat(cloud.getLaunchRetryWaitTime(), is(10));
        assertThat(cloud.getMaxLaunchRetries(), is(10));
        assertThat(cloud.getSkipTLSVerification(), is(false));
        assertThat(cloud.getSshLaunchDelaySeconds(), is(60));
        assertThat(cloud.getVmPollTime(), is(3000));
        
        List<? extends AnkaCloudSlaveTemplate> templates = cloud.getTemplates();
        assertThat(templates, hasSize(1));
        AnkaCloudSlaveTemplate template = templates.get(0);
        assertThat(template, notNullValue());
        
        assertThat(template.getCloudName(), is("Veertu anka"));
        assertThat(template.getCredentialsId(), is("anka-creds"));
        assertThat(template.getDescription(), is("macos anka template"));
        assertThat(template.isDeleteLatest(), is(false));
        assertThat(template.getDontAppendTimestamp(), is(false));
        assertThat(template.getKeepAliveOnError(), is(false));

        List<AnkaCloudSlaveTemplate.EnvironmentEntry> environments = template.getEnvironments();
        assertThat(environments, hasSize(1));
        AnkaCloudSlaveTemplate.EnvironmentEntry environment = environments.get(0);
        assertThat(environment.name, is("test-name"));
        assertThat(environment.value, is("test-value"));
        
        assertThat(template.getLabelString(), is("macos anka"));
        assertThat(template.getLaunchDelay(), is(0));
        assertThat(template.getLaunchMethod(), is("ssh"));
        assertThat(template.getMasterVmId(), is("xxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"));
        assertThat(template.getMode(), is(Mode.NORMAL));
        assertThat(template.getNameTemplate(), is("macos-anka"));
        assertThat(template.getNumberOfExecutors(), is(1));
        assertThat(template.getPriority(), is(0));
        assertThat(template.getPushTag(), is("test-tag"));
        assertThat(template.getRemoteFS(), is("/Users/anka"));
        
        RetentionStrategy<?> retentionStrategy = template.getRetentionStrategy();
        assertThat(retentionStrategy, notNullValue());
        assertThat(retentionStrategy, instanceOf(RunOnceCloudRetentionStrategy.class));
        RunOnceCloudRetentionStrategy runOnce = (RunOnceCloudRetentionStrategy) retentionStrategy;
        assertThat(runOnce.getIdleMinutes(), is(5));
        
        assertThat(template.getSaveImage(), is(Boolean.FALSE));
        
        SaveImageParameters saveImageParameters = template.getSaveImageParameters();
        assertThat(saveImageParameters, notNullValue());
        assertThat(saveImageParameters.getDescription(), is("macos anka template"));
        assertThat(saveImageParameters.isDeleteLatest(), is(false));
        assertThat(saveImageParameters.getDontAppendTimestamp(), is(false));
        assertThat(saveImageParameters.getTag(), is("test-tag"));
        assertThat(saveImageParameters.getTemplateID(), is("xxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"));
        assertThat(saveImageParameters.getSaveImage(), is(Boolean.FALSE));
        assertThat(saveImageParameters.getSuspend(), is(false));
        assertThat(saveImageParameters.getWaitForBuildToFinish(), is(false));

        assertThat(template.getSchedulingTimeout(), is(1800));
        assertThat(template.getSuspend(), is(false));
        assertThat(template.getTemplateId(), is("xxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"));
        assertThat(template.getWaitForBuildToFinish(), is(false));
        
        
    }
    
    private void validateCasCExport() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        final CNode cloud = getJenkinsRoot(context).get("clouds");
        String exported = toYamlString(cloud);
        String expected = toStringFromYamlFile(this, "expected_output.yml");
        assertThat(exported, is(expected));
        
    }
}
