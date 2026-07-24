package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.NodeGroup;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@WithJenkins
public class AbstractSlaveTemplateFetchGroupsTest {

    @Test
    public void fetchNodeGroupsForResolve_unknownCloud_throws(JenkinsRule j) {
        AbstractSlaveTemplate template = new AbstractSlaveTemplate();
        template.setCloudName("missing-cloud");
        template.setGroup("onprem");

        AnkaMgmtException thrown = assertThrows(AnkaMgmtException.class, template::fetchNodeGroupsForResolve);
        assertThat(thrown.getMessage(), containsString("cloud 'missing-cloud' was not found"));
    }

    @Test
    public void fetchNodeGroupsForResolve_knownCloud_returnsGroups(JenkinsRule j) throws Exception {
        AnkaMgmtCloud cloud = spy(new AnkaMgmtCloud(
                "http://127.0.0.1:9",
                "anka-fetch-groups",
                null,
                null,
                true,
                Collections.emptyList(),
                0));
        NodeGroup group = nodeGroup("group-uuid", "onprem");
        doReturn(Collections.singletonList(group)).when(cloud).listNodeGroups();
        j.jenkins.clouds.add(cloud);

        AbstractSlaveTemplate template = new AbstractSlaveTemplate();
        template.setCloudName("anka-fetch-groups");

        List<NodeGroup> groups = template.fetchNodeGroupsForResolve();
        assertThat(groups, hasSize(1));
        assertThat(groups.get(0).getName(), is("onprem"));
        assertThat(groups.get(0).getId(), is("group-uuid"));
    }

    private static NodeGroup nodeGroup(String id, String name) {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("description", "");
        return new NodeGroup(json);
    }
}
