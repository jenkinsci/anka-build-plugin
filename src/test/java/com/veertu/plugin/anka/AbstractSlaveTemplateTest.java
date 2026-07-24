package com.veertu.plugin.anka;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.veertu.ankaMgmtSdk.NodeGroup;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AbstractSlaveTemplateTest {

    private static final String OLD_UUID = "4894c60f-949d-4c5e-40d6-260c13bc0509";
    private static final String NEW_UUID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    private AbstractSlaveTemplate template;

    @BeforeEach
    public void setUp() {
        template = new AbstractSlaveTemplate();
        template.setCloudName("test-cloud");
    }

    @Test
    public void testSetGroupWithUUID_shouldKeepUUID() {
        template.setGroup(OLD_UUID);
        assertThat(template.getGroup(), is(OLD_UUID));
    }

    @Test
    public void testSetGroupWithNull_shouldKeepNull() {
        template.setGroup(null);
        assertThat(template.getGroup(), is(nullValue()));
    }

    @Test
    public void testSetGroupWithEmptyString_shouldReturnNull() {
        template.setGroup("");
        assertThat(template.getGroup(), is(nullValue()));
    }

    @Test
    public void testSetGroupWithWhitespace_shouldKeepWhitespace() {
        template.setGroup("   ");
        assertThat(template.getGroup(), is("   "));
    }

    @Test
    public void testSetGroupWithName_shouldStoreNameAsIs() {
        template.setGroup("onprem");
        assertThat(template.getGroup(), is("onprem"));
    }

    @Test
    public void testGetGroup_neverClearsStoredUuid() {
        template.setGroup(OLD_UUID);
        assertThat(template.getGroup(), is(OLD_UUID));
        assertThat(template.getGroup(), is(OLD_UUID));
    }

    @Test
    public void resolveGroupIdForStart_emptyGroup_returnsNull() throws Exception {
        ResolvingTemplate resolving = new ResolvingTemplate();
        resolving.setGroup(null);
        assertThat(resolving.resolveGroupIdForStart(), is(nullValue()));

        resolving.setGroup("");
        assertThat(resolving.resolveGroupIdForStart(), is(nullValue()));
    }

    @Test
    public void resolveGroupIdForStart_whitespaceOnly_throwsAndDoesNotClear() {
        ResolvingTemplate resolving = new ResolvingTemplate();
        resolving.setCloudName("test-cloud");
        resolving.setLabel("label-ssh");
        resolving.setGroup("   ");

        AnkaMgmtException thrown = assertThrows(AnkaMgmtException.class, resolving::resolveGroupIdForStart);
        assertThat(thrown.getMessage(), containsString("blank/whitespace"));
        assertThat(thrown.getMessage(), containsString("test-cloud"));
        assertThat(thrown.getMessage(), containsString("label-ssh"));
        assertThat(resolving.getGroup(), is("   "));
    }

    @Test
    public void fetchNodeGroupsForResolve_blankCloudName_throws() {
        template.setCloudName("  ");
        template.setGroup("onprem");

        AnkaMgmtException thrown = assertThrows(AnkaMgmtException.class, template::fetchNodeGroupsForResolve);
        assertThat(thrown.getMessage(), containsString("cloud name is not set"));
    }

    @Test
    public void fetchNodeGroupsForResolve_nullCloudName_throws() {
        template.setCloudName(null);
        template.setGroup("onprem");

        AnkaMgmtException thrown = assertThrows(AnkaMgmtException.class, template::fetchNodeGroupsForResolve);
        assertThat(thrown.getMessage(), containsString("cloud name is not set"));
    }

    @Test
    public void resolveGroupIdForStart_byName_afterRecreation_returnsNewUuid() throws Exception {
        ResolvingTemplate resolving = new ResolvingTemplate();
        resolving.setCloudName("test-cloud");
        resolving.setGroup("onprem");
        resolving.groupsToReturn = Collections.singletonList(nodeGroup(NEW_UUID, "onprem"));

        assertThat(resolving.resolveGroupIdForStart(), is(NEW_UUID));
        assertThat(resolving.getGroup(), is("onprem"));
    }

    @Test
    public void resolveGroupIdForStart_legacyUuidStillPresent_returnsUuid() throws Exception {
        ResolvingTemplate resolving = new ResolvingTemplate();
        resolving.setCloudName("test-cloud");
        resolving.setGroup(OLD_UUID);
        resolving.groupsToReturn = Arrays.asList(
                nodeGroup(OLD_UUID, "onprem"),
                nodeGroup(NEW_UUID, "other"));

        assertThat(resolving.resolveGroupIdForStart(), is(OLD_UUID));
        assertThat(resolving.getGroup(), is(OLD_UUID));
    }

    @Test
    public void resolveGroupIdForStart_apiFailure_throwsAndDoesNotClear() {
        ResolvingTemplate resolving = new ResolvingTemplate();
        resolving.setCloudName("test-cloud");
        resolving.setGroup("onprem");
        resolving.exceptionToThrow = new AnkaMgmtException("controller unavailable");

        assertThrows(AnkaMgmtException.class, resolving::resolveGroupIdForStart);
        assertThat(resolving.getGroup(), is("onprem"));
    }

    @Test
    public void resolveGroupIdForStart_authoritativeNotFound_throwsAndDoesNotClear() {
        ResolvingTemplate resolving = new ResolvingTemplate();
        resolving.setCloudName("test-cloud");
        resolving.setGroup(OLD_UUID);
        resolving.groupsToReturn = Collections.singletonList(nodeGroup(NEW_UUID, "onprem"));

        assertThrows(AnkaMgmtException.class, resolving::resolveGroupIdForStart);
        assertThat(resolving.getGroup(), is(OLD_UUID));
    }

    @Test
    public void resolveGroupIdForStart_nameNotFound_throwsAndDoesNotClear() {
        ResolvingTemplate resolving = new ResolvingTemplate();
        resolving.setCloudName("test-cloud");
        resolving.setGroup("missing-group");
        resolving.groupsToReturn = Collections.singletonList(nodeGroup(NEW_UUID, "onprem"));

        assertThrows(AnkaMgmtException.class, resolving::resolveGroupIdForStart);
        assertThat(resolving.getGroup(), is("missing-group"));
    }

    @Test
    public void javaPathDefaultsToNull() {
        assertThat(template.getJavaPath(), is(nullValue()));
    }

    @Test
    public void javaPathBlankBecomesNull() {
        template.setJavaPath("");
        assertThat(template.getJavaPath(), is(nullValue()));
    }

    @Test
    public void javaPathSetIsReturned() {
        template.setJavaPath("/usr/libexec/java_home/bin/java");
        assertThat(template.getJavaPath(), is("/usr/libexec/java_home/bin/java"));
    }

    @Test
    public void setPropertiesCopiesJavaPath() {
        DynamicSlaveProperties source = new DynamicSlaveProperties("vm-id");
        source.setJavaPath("/opt/java/bin/java");
        AnkaCloudSlaveTemplate dest = source.toSlaveTemplate();
        assertThat(dest.getJavaPath(), is("/opt/java/bin/java"));
    }

    @Test
    public void setPropertiesCopiesGroupNameAsIs() {
        DynamicSlaveProperties source = new DynamicSlaveProperties("vm-id");
        source.setGroup("onprem");
        AnkaCloudSlaveTemplate dest = source.toSlaveTemplate();
        assertThat(dest.getGroup(), is("onprem"));
    }

    private static NodeGroup nodeGroup(String id, String name) {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("description", "");
        return new NodeGroup(json);
    }

    /**
     * Test double that supplies node groups without a live Jenkins cloud.
     */
    private static final class ResolvingTemplate extends AbstractSlaveTemplate {
        List<NodeGroup> groupsToReturn = Collections.emptyList();
        AnkaMgmtException exceptionToThrow;

        @Override
        protected List<NodeGroup> fetchNodeGroupsForResolve() throws AnkaMgmtException {
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return groupsToReturn;
        }
    }
}
