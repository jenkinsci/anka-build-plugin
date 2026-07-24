package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaAPI;
import com.veertu.ankaMgmtSdk.NodeGroup;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WithJenkins
public class AnkaMgmtCloudListNodeGroupsTest {

    @Test
    public void listNodeGroups_nullApi_throws(JenkinsRule j) throws Exception {
        AnkaMgmtCloud cloud = new AnkaMgmtCloud(
                "http://127.0.0.1:9",
                "anka-list-groups-null",
                null,
                null,
                true,
                Collections.emptyList(),
                0);
        setAnkaApi(cloud, null);

        AnkaMgmtException thrown = assertThrows(AnkaMgmtException.class, cloud::listNodeGroups);
        assertThat(thrown.getMessage(), containsString("Anka API is not initialized"));
        assertThat(thrown.getMessage(), containsString("anka-list-groups-null"));
    }

    @Test
    public void listNodeGroups_delegatesToApi(JenkinsRule j) throws Exception {
        AnkaMgmtCloud cloud = new AnkaMgmtCloud(
                "http://127.0.0.1:9",
                "anka-list-groups-ok",
                null,
                null,
                true,
                Collections.emptyList(),
                0);

        AnkaAPI api = mock(AnkaAPI.class);
        when(api.getNodeGroups()).thenReturn(Collections.singletonList(nodeGroup("g1", "onprem")));
        setAnkaApi(cloud, api);

        List<NodeGroup> groups = cloud.listNodeGroups();
        assertThat(groups, hasSize(1));
        assertThat(groups.get(0).getId(), is("g1"));
        assertThat(groups.get(0).getName(), is("onprem"));
    }

    private static void setAnkaApi(AnkaMgmtCloud cloud, AnkaAPI api) throws Exception {
        Field field = AnkaMgmtCloud.class.getDeclaredField("ankaAPI");
        field.setAccessible(true);
        field.set(cloud, api);
    }

    private static NodeGroup nodeGroup(String id, String name) {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("description", "");
        return new NodeGroup(json);
    }
}
