package com.veertu.plugin.anka;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.sf.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class AnkaLabelsApiTest {

    private static final String CLOUD = "test-anka-cloud";
    private static final String CLOUD_B = "test-anka-cloud-b";
    private static final String TOKEN = "test-labels-api-token";
    private static final String TOKEN_B = "test-labels-api-token-b";
    private static final String TOKEN_CREDENTIAL_ID = "test-labels-api-token-cred";
    private static final String TOKEN_B_CREDENTIAL_ID = "test-labels-api-token-b-cred";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void labelsApiEndpointUrl_encodesCloudNameWithSpaces() {
        AnkaMgmtCloud cloud = new AnkaMgmtCloud(
                "https://127.0.0.1:9", "Anka Build Cloud", "", "", true, new ArrayList<>(), -1);

        assertThat(cloud.getLabelsApiEndpointUrl(), containsString("anka-build-cloud/labels/Anka%20Build%20Cloud"));
    }

    @Test
    public void replace_withValidToken_updatesTemplates() throws Exception {
        addCloudWithTemplates(List.of(baselineTemplate("L1", "vm-1"), baselineTemplate("L2", "vm-2")), true);
        String body =
                "{\"mode\":\"replace\",\"templates\":[" + singleTemplateJson("L1", "vm-replaced", "tag-x") + "]}";
        int code = postLabels(CLOUD, body, "Bearer " + TOKEN);
        assertThat(code, is(200));

        AnkaMgmtCloud updated = AnkaMgmtCloud.get(CLOUD);
        assertThat(updated.getTemplates().size(), is(1));
        assertThat(updated.getTemplates().get(0).getLabelString(), is("L1"));
        assertThat(updated.getTemplates().get(0).getMasterVmId(), is("vm-replaced"));
        assertThat(updated.getTemplates().get(0).getTag(), is("tag-x"));
    }

    @Test
    public void append_updatesMatchingLabelAndKeepsOthers() throws Exception {
        addCloudWithTemplates(List.of(baselineTemplate("L1", "vm-1"), baselineTemplate("L2", "vm-2")), true);
        String body =
                "{\"mode\":\"append\",\"templates\":[" + singleTemplateJson("L1", "vm-updated", "tag-y") + "]}";
        int code = postLabels(CLOUD, body, null, TOKEN);
        assertThat(code, is(200));

        AnkaMgmtCloud updated = AnkaMgmtCloud.get(CLOUD);
        assertThat(updated.getTemplates().size(), is(2));
        AnkaCloudSlaveTemplate t1 = updated.getTemplates().stream()
                .filter(t -> "L1".equals(t.getLabelString()))
                .findFirst()
                .orElseThrow();
        assertThat(t1.getMasterVmId(), is("vm-updated"));
        assertThat(t1.getTag(), is("tag-y"));
        AnkaCloudSlaveTemplate t2 = updated.getTemplates().stream()
                .filter(t -> "L2".equals(t.getLabelString()))
                .findFirst()
                .orElseThrow();
        assertThat(t2.getMasterVmId(), is("vm-2"));
    }

    @Test
    public void wrongToken_returns401() throws Exception {
        addCloudWithTemplates(List.of(baselineTemplate("L1", "vm-1")), true);
        int code = postLabels(CLOUD, "{\"mode\":\"replace\",\"templates\":[]}", "Bearer wrong");
        assertThat(code, is(401));
    }

    @Test
    public void secondCloudUrl_rejectsFirstCloudToken_returns401() throws Exception {
        addCloudWithTemplates(List.of(baselineTemplate("L1", "vm-1")), true);
        addNamedCloud(
                CLOUD_B,
                List.of(baselineTemplateForCloud(CLOUD_B, "L1", "vm-b")),
                TOKEN_B_CREDENTIAL_ID);
        String body = "{\"mode\":\"replace\",\"templates\":["
                + singleTemplateJson(CLOUD_B, "L1", "vm-updated", "t")
                + "]}";
        int code = postLabels(CLOUD_B, body, "Bearer " + TOKEN);
        assertThat(code, is(401));
    }

    @Test
    public void firstCloudUrl_rejectsSecondCloudToken_returns401() throws Exception {
        addCloudWithTemplates(List.of(baselineTemplate("L1", "vm-1")), true);
        addNamedCloud(
                CLOUD_B,
                List.of(baselineTemplateForCloud(CLOUD_B, "L1", "vm-b")),
                TOKEN_B_CREDENTIAL_ID);
        String body = "{\"mode\":\"replace\",\"templates\":["
                + singleTemplateJson("L1", "vm-updated", "t")
                + "]}";
        int code = postLabels(CLOUD, body, "Bearer " + TOKEN_B);
        assertThat(code, is(401));
    }

    @Test
    public void missingToken_returns401() throws Exception {
        addCloudWithTemplates(List.of(baselineTemplate("L1", "vm-1")), true);
        int code = postLabels(CLOUD, "{\"mode\":\"replace\",\"templates\":[]}", null);
        assertThat(code, is(401));
    }

    @Test
    public void whenApiDisabled_returns503() throws Exception {
        addCloudWithTemplates(List.of(baselineTemplate("L1", "vm-1")), false);
        int code = postLabels(CLOUD, "{\"mode\":\"replace\",\"templates\":[]}", "Bearer " + TOKEN);
        assertThat(code, is(503));
    }

    @Test
    public void unknownCloud_returns404() throws Exception {
        addCloudWithTemplates(List.of(baselineTemplate("L1", "vm-1")), true);
        int code = postLabels("no-such-cloud", "{\"mode\":\"replace\",\"templates\":[]}", "Bearer " + TOKEN);
        assertThat(code, is(404));
    }

    @Test
    public void nonPostMethod_isRejected() throws Exception {
        addCloudWithTemplates(List.of(baselineTemplate("L1", "vm-1")), true);
        URL url = new URL(j.getURL(), "anka-build-cloud/labels/" + CLOUD);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("GET");
        try {
            int code = c.getResponseCode();
            assertThat(code, anyOf(is(404), is(405)));
        } finally {
            c.disconnect();
        }
    }

    @Test
    public void duplicateLabelsInPayload_returns400() throws Exception {
        addCloudWithTemplates(List.of(baselineTemplate("L1", "vm-1")), true);
        String dup = "{\"mode\":\"replace\",\"templates\":["
                + singleTemplateJson("L1", "a", "t")
                + ","
                + singleTemplateJson("L1", "b", "t")
                + "]}";
        int code = postLabels(CLOUD, dup, "Bearer " + TOKEN);
        assertThat(code, is(400));
    }

    @Test
    public void invalidMode_returns400() throws Exception {
        addCloudWithTemplates(List.of(baselineTemplate("L1", "vm-1")), true);
        String body = "{\"mode\":\"nope\",\"templates\":[" + singleTemplateJson("L1", "vm-1", "t") + "]}";
        int code = postLabels(CLOUD, body, "Bearer " + TOKEN);
        assertThat(code, is(400));
    }

    @Test
    public void templateBindWithWrongFieldType_returns400() throws Exception {
        addCloudWithTemplates(List.of(baselineTemplate("L1", "vm-1")), true);
        String bad = singleTemplateJson("L1", "vm-1", "t").replace("\"numberOfExecutors\":1", "\"numberOfExecutors\":\"not-int\"");
        String body = "{\"mode\":\"replace\",\"templates\":[" + bad + "]}";
        int code = postLabels(CLOUD, body, "Bearer " + TOKEN);
        assertThat(code, is(400));
    }

    @Test
    public void templateCloudName_isSetFromTargetCloud() throws Exception {
        addCloudWithTemplates(List.of(baselineTemplate("L1", "vm-1")), true);
        String body = "{\"mode\":\"replace\",\"templates\":["
                + singleTemplateJson(CLOUD_B, "L1", "vm-replaced", "t")
                + "]}";
        int code = postLabels(CLOUD, body, "Bearer " + TOKEN);
        assertThat(code, is(200));

        AnkaMgmtCloud updated = AnkaMgmtCloud.get(CLOUD);
        assertThat(updated.getTemplates().size(), is(1));
        assertThat(updated.getTemplates().get(0).getCloudName(), is(CLOUD));
    }

    @Test
    public void oversizedBody_returns413() throws Exception {
        addCloudWithTemplates(List.of(baselineTemplate("L1", "vm-1")), true);
        String prefix = "{\"mode\":\"replace\",\"templates\":[{\"label\":\"L1\",\"masterVmId\":\"vm-1\",\"templateDescription\":\"";
        String suffix = "\"}]}";
        int paddingLength = AnkaLabelsApiRootAction.MAX_REQUEST_BODY_BYTES - prefix.length() - suffix.length() + 1;
        String body = prefix + "x".repeat(paddingLength) + suffix;
        int code = postLabels(CLOUD, body, "Bearer " + TOKEN);
        assertThat(code, is(413));
    }

    @Test
    public void responseBody_onSuccess_isJsonSummary() throws Exception {
        addCloudWithTemplates(List.of(baselineTemplate("L1", "vm-1")), true);
        String body = "{\"mode\":\"replace\",\"templates\":[" + singleTemplateJson("L1", "vm-x", "t") + "]}";
        HttpURLConnection conn = openPost(CLOUD, body, "Bearer " + TOKEN, null);
        try {
            assertThat(conn.getResponseCode(), is(200));
            String text = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JSONObject o = JSONObject.fromObject(text);
            assertThat(o.getString("cloudName"), is(CLOUD));
            assertThat(o.getString("mode"), is("replace"));
            assertThat(o.getInt("previousCount"), is(1));
            assertThat(o.getInt("newCount"), is(1));
        } finally {
            conn.disconnect();
        }
    }

    private void addCloudWithTemplates(List<AnkaCloudSlaveTemplate> templates, boolean enableApi) throws Exception {
        addNamedCloud(CLOUD, templates, enableApi ? TOKEN_CREDENTIAL_ID : null);
    }

    private void addNamedCloud(String name, List<AnkaCloudSlaveTemplate> templates, String labelsApiTokenCredentialIdOrNull)
            throws Exception {
        if (labelsApiTokenCredentialIdOrNull != null) {
            String token = TOKEN_CREDENTIAL_ID.equals(labelsApiTokenCredentialIdOrNull) ? TOKEN : TOKEN_B;
            addLabelsApiTokenCredential(labelsApiTokenCredentialIdOrNull, token);
        }
        AnkaMgmtCloud cloud =
                new AnkaMgmtCloud("https://127.0.0.1:9", name, "", "", true, templates, -1);
        if (labelsApiTokenCredentialIdOrNull != null) {
            cloud.setLabelsApiTokenCredentialsId(labelsApiTokenCredentialIdOrNull);
        }
        j.jenkins.clouds.add(cloud);
        j.jenkins.save();
    }

    private void addLabelsApiTokenCredential(String credentialId, String token) throws Exception {
        CredentialsProvider.lookupStores(j.jenkins)
                .iterator()
                .next()
                .addCredentials(
                        Domain.global(),
                        new AnkaLabelsApiTokenCredentials(
                                CredentialsScope.GLOBAL,
                                credentialId,
                                AnkaCredentialNaming.PREFIX + credentialId,
                                token));
    }

    private static AnkaCloudSlaveTemplate baselineTemplate(String label, String masterVmId) {
        return baselineTemplateForCloud(CLOUD, label, masterVmId);
    }

    private static AnkaCloudSlaveTemplate baselineTemplateForCloud(
            String cloudName, String label, String masterVmId) {
        return new AnkaCloudSlaveTemplate(
                cloudName,
                "/tmp/fs",
                masterVmId,
                "baseline-tag",
                label,
                "desc",
                1,
                0,
                false,
                "ssh",
                "",
                "n-" + label,
                0,
                1800,
                0,
                0,
                false,
                masterVmId,
                null,
                null,
                null,
                null,
                false,
                null,
                new ArrayList<>());
    }

    private static String singleTemplateJson(String label, String masterVmId, String tag) {
        return singleTemplateJson(CLOUD, label, masterVmId, tag);
    }

    private static String singleTemplateJson(String cloudName, String label, String masterVmId, String tag) {
        return "{"
                + "\"cloudName\":\"" + cloudName + "\","
                + "\"remoteFS\":\"/tmp/fs\","
                + "\"masterVmId\":\"" + masterVmId + "\","
                + "\"tag\":\"" + tag + "\","
                + "\"label\":\"" + label + "\","
                + "\"templateDescription\":\"desc\","
                + "\"numberOfExecutors\":1,"
                + "\"launchDelay\":0,"
                + "\"keepAliveOnError\":false,"
                + "\"launchMethod\":\"jnlp\","
                + "\"credentialsId\":\"\","
                + "\"group\":\"\","
                + "\"nameTemplate\":\"n\","
                + "\"priority\":0,"
                + "\"schedulingTimeout\":1800,"
                + "\"vcpu\":0,"
                + "\"vram\":0,"
                + "\"saveImage\":false,"
                + "\"templateId\":\"" + masterVmId + "\","
                + "\"saveImageParameters\":{"
                + "\"saveImage\":false,"
                + "\"templateID\":\"" + masterVmId + "\","
                + "\"tag\":\"" + tag + "\","
                + "\"dontAppendTimestamp\":false,"
                + "\"deleteLatest\":false,"
                + "\"description\":\"desc\","
                + "\"suspend\":false,"
                + "\"waitForBuildToFinish\":false"
                + "},"
                + "\"environments\":[]"
                + "}";
    }

    private int postLabels(String cloudName, String jsonBody, String authorization) throws IOException {
        return postLabels(cloudName, jsonBody, authorization, null);
    }

    private int postLabels(String cloudName, String jsonBody, String authorization, String xAnkaToken)
            throws IOException {
        HttpURLConnection c = openPost(cloudName, jsonBody, authorization, xAnkaToken);
        try {
            int code = c.getResponseCode();
            drainConnection(c);
            return code;
        } finally {
            c.disconnect();
        }
    }

    private static void drainConnection(HttpURLConnection c) throws IOException {
        java.io.InputStream in = c.getErrorStream() != null ? c.getErrorStream() : c.getInputStream();
        if (in != null) {
            in.readAllBytes();
            in.close();
        }
    }

    private HttpURLConnection openPost(String cloudName, String jsonBody, String authorization, String xAnkaToken)
            throws IOException {
        URL url = new URL(j.getURL(), "anka-build-cloud/labels/" + cloudName);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        if (authorization != null) {
            c.setRequestProperty("Authorization", authorization);
        }
        if (xAnkaToken != null) {
            c.setRequestProperty("X-Anka-Labels-Token", xAnkaToken);
        }
        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        c.getOutputStream().write(bytes);
        c.getOutputStream().flush();
        return c;
    }
}
