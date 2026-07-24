package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.NodeGroup;
import hudson.util.ListBoxModel;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@WithJenkins
public class AnkaCloudSlaveTemplateGroupFillTest {

    @Test
    public void doFillGroupItems_whenOnline_persistsGroupNames(JenkinsRule j) throws Exception {
        AnkaMgmtCloud cloud = spy(new AnkaMgmtCloud(
                "http://127.0.0.1:9",
                "anka-group-fill",
                null,
                null,
                true,
                Collections.emptyList(),
                0));
        doReturn(true).when(cloud).isOnline();
        doReturn(Arrays.asList(
                nodeGroup("uuid-b", "beta-group"),
                nodeGroup("uuid-a", "alpha-group"))).when(cloud).getNodeGroups();
        j.jenkins.clouds.add(cloud);

        AnkaCloudSlaveTemplate.DescriptorImpl descriptor =
                j.jenkins.getDescriptorByType(AnkaCloudSlaveTemplate.DescriptorImpl.class);
        ListBoxModel options = descriptor.doFillGroupItems("anka-group-fill");

        List<String> values = options.stream().map(option -> option.value).collect(Collectors.toList());
        assertThat(values, contains("", "alpha-group", "beta-group"));
        assertThat(options.get(1).name, is("alpha-group"));
        assertThat(options.get(1).value, is("alpha-group"));
        assertThat(options.get(2).name, is("beta-group"));
        assertThat(options.get(2).value, is("beta-group"));
    }

    private static NodeGroup nodeGroup(String id, String name) {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("description", "");
        return new NodeGroup(json);
    }
}
