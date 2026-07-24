package com.veertu.plugin.anka;

import hudson.model.Node;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;

@WithJenkins
public class AnkaMgmtCloudNullTemplatesTest {

    @Test
    @Issue("https://github.com/jenkinsci/anka-build-plugin/issues/80")
    public void missingTemplatesInXmlDefaultsToEmptyList(JenkinsRule j) {
        String xml = ""
                + "<com.veertu.plugin.anka.AnkaMgmtCloud>"
                + "  <name>Anka OB</name>"
                + "  <ankaMgmtUrl>https://example:8443/</ankaMgmtUrl>"
                + "  <credentialsId>creds</credentialsId>"
                + "  <skipTLSVerification>true</skipTLSVerification>"
                + "  <cloudInstanceCap>-1</cloudInstanceCap>"
                + "</com.veertu.plugin.anka.AnkaMgmtCloud>";

        AnkaMgmtCloud cloud = (AnkaMgmtCloud) Jenkins.XSTREAM2.fromXML(xml);

        assertThat(cloud.getTemplates(), notNullValue());
        assertThat(cloud.getTemplates(), hasSize(0));
    }

    @Test
    public void readResolveKeepsTemplatesWhenPresent(JenkinsRule j) {
        String xml = ""
                + "<com.veertu.plugin.anka.AnkaMgmtCloud>"
                + "  <name>Anka With Templates</name>"
                + "  <ankaMgmtUrl>https://example:8443/</ankaMgmtUrl>"
                + "  <credentialsId>creds</credentialsId>"
                + "  <skipTLSVerification>true</skipTLSVerification>"
                + "  <cloudInstanceCap>-1</cloudInstanceCap>"
                + "  <templates>"
                + "    <com.veertu.plugin.anka.AnkaCloudSlaveTemplate>"
                + "      <masterVmId>vm-1</masterVmId>"
                + "      <labelString>label-ssh</labelString>"
                + "      <remoteFS>/Users/anka</remoteFS>"
                + "      <numberOfExecutors>1</numberOfExecutors>"
                + "      <mode>NORMAL</mode>"
                + "      <launchMethod>ssh</launchMethod>"
                + "    </com.veertu.plugin.anka.AnkaCloudSlaveTemplate>"
                + "  </templates>"
                + "</com.veertu.plugin.anka.AnkaMgmtCloud>";

        AnkaMgmtCloud cloud = (AnkaMgmtCloud) Jenkins.XSTREAM2.fromXML(xml);

        assertThat(cloud.getTemplates(), hasSize(1));
        assertThat(cloud.getTemplates().get(0).getMasterVmId(), is("vm-1"));
        assertThat(cloud.getTemplates().get(0).getLabel(), is("label-ssh"));
    }

    @Test
    public void getTemplatesInitializesNullField(JenkinsRule j) throws Exception {
        AnkaCloudSlaveTemplate template = new AnkaCloudSlaveTemplate();
        template.setMasterVmId("vm-1");
        template.setLabel("label-ssh");
        template.setRemoteFS("/Users/anka");
        template.setNumberOfExecutors(1);
        template.setMode(Node.Mode.NORMAL);
        template.setLaunchMethod(LaunchMethod.SSH);

        AnkaMgmtCloud cloud = new AnkaMgmtCloud(
                "https://example:8443/",
                "anka-null-get",
                null,
                null,
                true,
                Collections.singletonList(template),
                0);

        Field templatesField = AnkaMgmtCloud.class.getDeclaredField("templates");
        templatesField.setAccessible(true);
        templatesField.set(cloud, null);

        List<AnkaCloudSlaveTemplate> templates = cloud.getTemplates();
        assertThat(templates, notNullValue());
        assertThat(templates, hasSize(0));
        assertThat(cloud.getTemplates(), sameInstance(templates));
    }
}
