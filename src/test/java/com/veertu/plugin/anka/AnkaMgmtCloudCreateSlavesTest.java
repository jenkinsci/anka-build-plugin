package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaAPI;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import hudson.model.Node;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WithJenkins
public class AnkaMgmtCloudCreateSlavesTest {

    @Test
    public void createNewDurableSlaves_resolvesGroupBeforeStartVm(JenkinsRule j) throws Exception {
        AnkaMgmtCloud cloud = newCloud("anka-create-durable");
        AnkaAPI api = mock(AnkaAPI.class);
        when(api.startVM(
                anyString(), isNull(), isNull(), isNull(), anyInt(), anyString(), anyString(), anyInt(), anyInt()))
                .thenThrow(new AnkaMgmtException("start failed"));
        setAnkaApi(cloud, api);

        AnkaCloudSlaveTemplate template = sshTemplate(cloud.getCloudName());
        List<?> slaves = invokeCreateSlaves(cloud, "createNewDurableSlaves", template, 1);

        assertThat(slaves, empty());
        verify(api).startVM(
                anyString(), isNull(), isNull(), isNull(), anyInt(), anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    public void createNewLightWeightSlaves_resolvesGroupBeforeStartVm(JenkinsRule j) throws Exception {
        AnkaMgmtCloud cloud = newCloud("anka-create-light");
        AnkaAPI api = mock(AnkaAPI.class);
        when(api.startVM(
                anyString(), isNull(), isNull(), isNull(), anyInt(), anyString(), anyString(), anyInt(), anyInt()))
                .thenThrow(new AnkaMgmtException("start failed"));
        setAnkaApi(cloud, api);

        AnkaCloudSlaveTemplate template = sshTemplate(cloud.getCloudName());

        InvocationTargetException thrown = assertThrows(
                InvocationTargetException.class,
                () -> invokeCreateSlaves(cloud, "createNewLightWeightSlaves", template, 1));
        assertThat(thrown.getCause(), instanceOf(AnkaMgmtException.class));
        verify(api).startVM(
                anyString(), isNull(), isNull(), isNull(), anyInt(), anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    public void createNewDurableSlaves_withGroupName_passesResolvedUuid(JenkinsRule j) throws Exception {
        AnkaMgmtCloud cloud = newCloud("anka-create-group");
        AnkaAPI api = mock(AnkaAPI.class);
        when(api.getNodeGroups()).thenReturn(Collections.singletonList(nodeGroup(
                "4894c60f-949d-4c5e-40d6-260c13bc0509", "onprem")));
        when(api.startVM(
                anyString(), isNull(), isNull(), anyString(), anyInt(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn("vm-instance-1");
        setAnkaApi(cloud, api);
        j.jenkins.clouds.add(cloud);

        AnkaCloudSlaveTemplate template = sshTemplate(cloud.getCloudName());
        template.setGroup("onprem");
        template.setRetentionStrategy(new RunOnceCloudRetentionStrategy(5));

        List<?> slaves = invokeCreateSlaves(cloud, "createNewDurableSlaves", template, 1);
        assertThat(slaves, hasSize(1));
        verify(api).startVM(
                anyString(),
                isNull(),
                isNull(),
                org.mockito.ArgumentMatchers.eq("4894c60f-949d-4c5e-40d6-260c13bc0509"),
                anyInt(),
                anyString(),
                anyString(),
                anyInt(),
                anyInt());
    }

    private static AnkaMgmtCloud newCloud(String name) {
        return new AnkaMgmtCloud(
                "http://127.0.0.1:9",
                name,
                null,
                null,
                true,
                Collections.emptyList(),
                0);
    }

    private static AnkaCloudSlaveTemplate sshTemplate(String cloudName) {
        AnkaCloudSlaveTemplate template = new AnkaCloudSlaveTemplate();
        template.setCloudName(cloudName);
        template.setMasterVmId("master-vm");
        template.setLabel("label-ssh");
        template.setRemoteFS("/Users/anka");
        template.setNumberOfExecutors(1);
        template.setMode(Node.Mode.NORMAL);
        template.setLaunchMethod(LaunchMethod.SSH);
        template.setRetentionStrategy(new RunOnceCloudRetentionStrategy(5));
        return template;
    }

    private static List<?> invokeCreateSlaves(
            AnkaMgmtCloud cloud, String methodName, AnkaCloudSlaveTemplate template, int number) throws Exception {
        Method method = AnkaMgmtCloud.class.getDeclaredMethod(methodName, AnkaCloudSlaveTemplate.class, int.class);
        method.setAccessible(true);
        return (List<?>) method.invoke(cloud, template, number);
    }

    private static void setAnkaApi(AnkaMgmtCloud cloud, AnkaAPI api) throws Exception {
        Field field = AnkaMgmtCloud.class.getDeclaredField("ankaAPI");
        field.setAccessible(true);
        field.set(cloud, api);
    }

    private static com.veertu.ankaMgmtSdk.NodeGroup nodeGroup(String id, String name) {
        org.json.JSONObject json = new org.json.JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("description", "");
        return new com.veertu.ankaMgmtSdk.NodeGroup(json);
    }
}
