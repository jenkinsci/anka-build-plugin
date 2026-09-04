package com.veertu.plugin.anka;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.Run;
import jenkins.model.JenkinsLocationConfiguration;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@WithJenkins
public class JobMetadataTest {

    @Test
    public void resolveAbsoluteJobUrl_returnsNullForNullRun(JenkinsRule j) {
        assertThat(AnkaCloudComputer.resolveAbsoluteJobUrl(null), nullValue());
    }

    @Test
    public void resolveAbsoluteJobUrl_returnsAbsoluteBuildUrl(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("metadata-job");
        FreeStyleBuild build = j.buildAndAssertSuccess(project);

        String absoluteUrl = AnkaCloudComputer.resolveAbsoluteJobUrl(build);

        assertThat(absoluteUrl, startsWith(j.jenkins.getRootUrl()));
        assertThat(absoluteUrl, containsString("/job/metadata-job/"));
        assertThat(absoluteUrl, containsString("/" + build.getNumber() + "/"));
    }

    @Test
    public void resolveAbsoluteJobUrl_returnsNullWhenRootUrlUnset(JenkinsRule j) {
        JenkinsLocationConfiguration location = JenkinsLocationConfiguration.get();
        String previousRootUrl = location.getUrl();
        location.setUrl(null);
        try {
            Run<?, ?> run = mock(Run.class);
            assertThat(AnkaCloudComputer.resolveAbsoluteJobUrl(run), nullValue());
        } finally {
            location.setUrl(previousRootUrl);
        }
    }

    @Test
    public void setJobNameAndNumber_forwardsJobIdAndUrlToUpdateInstance(JenkinsRule j) throws Exception {
        AnkaMgmtCloud cloud = mock(AnkaMgmtCloud.class);
        AnkaCloudSlaveTemplate template = new AnkaCloudSlaveTemplate();
        template.setCloudName("anka-meta");
        template.setRemoteFS("/tmp");
        template.setLaunchMethod(LaunchMethod.SSH);
        template.setRetentionStrategy(new RunOnceCloudRetentionStrategy(5));

        AbstractAnkaSlave slave = new AnkaOnDemandSlave(
                cloud,
                "meta-node",
                "desc",
                "/tmp",
                1,
                Node.Mode.NORMAL,
                "label",
                new AnkaLauncher(cloud, template, "vm-meta-1"),
                Collections.emptyList(),
                template,
                "vm-meta-1");

        slave.setJobNameAndNumber(
                "folder/job #9",
                "https://jenkins.example/job/folder/job/9/");

        assertThat(slave.getJobNameAndNumber(), is("folder/job #9"));
        verify(cloud).updateInstance(
                eq("vm-meta-1"),
                isNull(),
                isNull(),
                eq("folder/job #9"),
                eq("https://jenkins.example/job/folder/job/9/"));
    }

    @Test
    public void setJobNameAndNumber_withoutUrl_passesNullJobUrl(JenkinsRule j) throws Exception {
        AnkaMgmtCloud cloud = mock(AnkaMgmtCloud.class);
        AnkaCloudSlaveTemplate template = new AnkaCloudSlaveTemplate();
        template.setCloudName("anka-meta");
        template.setRemoteFS("/tmp");
        template.setLaunchMethod(LaunchMethod.SSH);
        template.setRetentionStrategy(new RunOnceCloudRetentionStrategy(5));

        AbstractAnkaSlave slave = new AnkaOnDemandSlave(
                cloud,
                "meta-node-2",
                "desc",
                "/tmp",
                1,
                Node.Mode.NORMAL,
                "label",
                new AnkaLauncher(cloud, template, "vm-meta-2"),
                Collections.emptyList(),
                template,
                "vm-meta-2");

        slave.setJobNameAndNumber("job #3");

        verify(cloud).updateInstance(
                eq("vm-meta-2"),
                isNull(),
                isNull(),
                eq("job #3"),
                isNull());
    }
}
