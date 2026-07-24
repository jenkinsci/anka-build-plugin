package com.veertu.plugin.anka;

import jenkins.model.Jenkins;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

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
}
