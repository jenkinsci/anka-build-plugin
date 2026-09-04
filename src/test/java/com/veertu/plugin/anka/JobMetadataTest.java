package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaAPI;
import com.veertu.ankaMgmtSdk.AnkaVmInstance;
import hudson.model.Executor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.queue.WorkUnit;
import jenkins.model.JenkinsLocationConfiguration;
import org.jenkinsci.plugins.workflow.support.steps.ExecutorStepExecution;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        AbstractAnkaSlave slave = newSlave(cloud, "meta-node", "vm-meta-1");

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
        AbstractAnkaSlave slave = newSlave(cloud, "meta-node-2", "vm-meta-2");

        slave.setJobNameAndNumber("job #3");

        verify(cloud).updateInstance(
                eq("vm-meta-2"),
                isNull(),
                isNull(),
                eq("job #3"),
                isNull());
    }

    @Test
    public void applyAcceptedRunMetadata_sendsJobIdAndAbsoluteUrl(JenkinsRule j) throws Exception {
        AnkaMgmtCloud cloud = mockCloudWithInstance();
        AbstractAnkaSlave slave = newSlave(cloud, "meta-run", "vm-run");
        AnkaCloudComputer computer = new AnkaCloudComputer(slave, "vm-run");
        FreeStyleProject project = j.createFreeStyleProject("pipeline-like-job");
        FreeStyleBuild build = j.buildAndAssertSuccess(project);

        computer.applyAcceptedRunMetadata(build);

        verify(cloud).updateInstance(
                eq("vm-run"),
                isNull(),
                isNull(),
                eq(build.getFullDisplayName()),
                eq(build.getAbsoluteUrl()));
    }

    @Test
    public void applyAcceptedRunMetadata_nullRunDoesNothing(JenkinsRule j) throws Exception {
        AnkaMgmtCloud cloud = mockCloudWithInstance();
        AbstractAnkaSlave slave = newSlave(cloud, "meta-null-run", "vm-null-run");
        AnkaCloudComputer computer = new AnkaCloudComputer(slave, "vm-null-run");

        computer.applyAcceptedRunMetadata(null);

        assertThat(slave.getJobNameAndNumber(), nullValue());
    }

    @Test
    public void applyAcceptedExecutableMetadata_runSendsAbsoluteUrl(JenkinsRule j) throws Exception {
        AnkaMgmtCloud cloud = mockCloudWithInstance();
        AbstractAnkaSlave slave = newSlave(cloud, "meta-exec", "vm-exec");
        AnkaCloudComputer computer = new AnkaCloudComputer(slave, "vm-exec");
        FreeStyleProject project = j.createFreeStyleProject("freestyle-meta");
        FreeStyleBuild build = j.buildAndAssertSuccess(project);

        computer.applyAcceptedExecutableMetadata(build.toString(), build, project);

        verify(cloud).updateInstance(
                eq("vm-exec"),
                isNull(),
                isNull(),
                eq(build.toString()),
                eq(build.getAbsoluteUrl()));
    }

    @Test
    public void applyAcceptedExecutableMetadata_nonRunOmitsUrl(JenkinsRule j) throws Exception {
        AnkaMgmtCloud cloud = mockCloudWithInstance();
        AbstractAnkaSlave slave = newSlave(cloud, "meta-nonrun", "vm-nonrun");
        AnkaCloudComputer computer = new AnkaCloudComputer(slave, "vm-nonrun");
        Queue.Executable executable = mock(Queue.Executable.class);
        when(executable.toString()).thenReturn("custom-task");

        computer.applyAcceptedExecutableMetadata("custom-task", executable, mock(Queue.Task.class));

        verify(cloud).updateInstance(
                eq("vm-nonrun"),
                isNull(),
                isNull(),
                eq("custom-task"),
                isNull());
    }

    @Test
    public void taskAccepted_pipelinePlaceholder_sendsJobUrl(JenkinsRule j) throws Exception {
        AnkaMgmtCloud cloud = mockCloudWithInstance();
        AbstractAnkaSlave slave = newSlave(cloud, "meta-pipe", "vm-pipe");
        j.jenkins.addNode(slave);
        AnkaCloudComputer computer = (AnkaCloudComputer) slave.toComputer();
        FreeStyleProject project = j.createFreeStyleProject("placeholder-meta");
        FreeStyleBuild build = j.buildAndAssertSuccess(project);

        ExecutorStepExecution.PlaceholderTask placeholder = mock(ExecutorStepExecution.PlaceholderTask.class);
        org.mockito.Mockito.doReturn(build).when(placeholder).run();
        Executor executor = mock(Executor.class);
        when(executor.getOwner()).thenReturn(computer);

        computer.taskAccepted(executor, placeholder);

        verify(cloud).updateInstance(
                eq("vm-pipe"),
                isNull(),
                isNull(),
                eq(build.getFullDisplayName()),
                eq(build.getAbsoluteUrl()));
    }

    @Test
    public void taskAccepted_freestyleRunExecutable_sendsJobUrl(JenkinsRule j) throws Exception {
        AnkaMgmtCloud cloud = mockCloudWithInstance();
        AbstractAnkaSlave slave = newSlave(cloud, "meta-fs", "vm-fs");
        j.jenkins.addNode(slave);
        AnkaCloudComputer computer = (AnkaCloudComputer) slave.toComputer();
        FreeStyleProject project = j.createFreeStyleProject("fs-meta");
        FreeStyleBuild build = j.buildAndAssertSuccess(project);

        WorkUnit workUnit = mock(WorkUnit.class);
        when(workUnit.getExecutable()).thenReturn(build);
        Executor executor = mock(Executor.class);
        when(executor.getOwner()).thenReturn(computer);
        when(executor.getCurrentWorkUnit()).thenReturn(workUnit);

        computer.taskAccepted(executor, project);

        verify(cloud).updateInstance(
                eq("vm-fs"),
                isNull(),
                isNull(),
                eq(build.toString()),
                eq(build.getAbsoluteUrl()));
    }

    @Test
    public void updateInstance_onCloud_forwardsJobUrlToApi(JenkinsRule j) throws Exception {
        AnkaMgmtCloud cloud = new AnkaMgmtCloud(
                "http://127.0.0.1:9",
                "anka-meta-fwd",
                null,
                null,
                true,
                Collections.emptyList(),
                0);
        AnkaAPI api = mock(AnkaAPI.class);
        setAnkaApi(cloud, api);

        cloud.updateInstance("vm-fwd", null, null, "job #1", "https://jenkins.example/job/job/1/");

        verify(api).updateInstance(
                eq("vm-fwd"),
                isNull(),
                isNull(),
                eq("job #1"),
                eq("https://jenkins.example/job/job/1/"));
    }

    private static AnkaMgmtCloud mockCloudWithInstance() throws Exception {
        AnkaMgmtCloud cloud = mock(AnkaMgmtCloud.class);
        AnkaVmInstance instance = mock(AnkaVmInstance.class);
        when(instance.getVmId()).thenReturn("tmpl-1");
        when(instance.getName()).thenReturn("vm-name");
        when(instance.getVmInfo()).thenReturn(null);
        when(cloud.showInstance(anyString())).thenReturn(instance);
        return cloud;
    }

    private static AbstractAnkaSlave newSlave(AnkaMgmtCloud cloud, String nodeName, String vmId) throws Exception {
        AnkaCloudSlaveTemplate template = new AnkaCloudSlaveTemplate();
        template.setCloudName("anka-meta");
        template.setRemoteFS("/tmp");
        template.setLaunchMethod(LaunchMethod.SSH);
        template.setRetentionStrategy(new RunOnceCloudRetentionStrategy(5));
        return new AnkaOnDemandSlave(
                cloud,
                nodeName,
                "desc",
                "/tmp",
                1,
                Node.Mode.NORMAL,
                "label",
                new AnkaLauncher(cloud, template, vmId),
                Collections.emptyList(),
                template,
                vmId);
    }

    private static void setAnkaApi(AnkaMgmtCloud cloud, AnkaAPI api) throws Exception {
        Field field = AnkaMgmtCloud.class.getDeclaredField("ankaAPI");
        field.setAccessible(true);
        field.set(cloud, api);
    }
}
