package com.veertu.ankaMgmtSdk;

import com.veertu.plugin.anka.MetadataKeys;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AnkaMgmtCommunicatorUpdateVmTest {

    @Test
    public void updateVM_includesJobIdAndJobUrlInMetadata() throws Exception {
        AtomicReference<JSONObject> capturedBody = new AtomicReference<>();
        AnkaMgmtCommunicator communicator = capturingCommunicator(capturedBody);

        communicator.updateVM("vm-1", null, null, "folder/job #7", "https://jenkins.example/job/folder/job/7/");

        JSONObject body = capturedBody.get();
        assertThat(body.has("external_id"), is(false));
        JSONObject metadata = body.getJSONObject("metadata");
        assertThat(metadata.getString(MetadataKeys.JOB_IDENTIFIER), is("folder/job #7"));
        assertThat(metadata.getString(MetadataKeys.JOB_URL), is("https://jenkins.example/job/folder/job/7/"));
    }

    @Test
    public void updateVM_omitsJobUrlKeyWhenNull() throws Exception {
        AtomicReference<JSONObject> capturedBody = new AtomicReference<>();
        AnkaMgmtCommunicator communicator = capturingCommunicator(capturedBody);

        communicator.updateVM("vm-1", null, null, "job #1", null);

        JSONObject metadata = capturedBody.get().getJSONObject("metadata");
        assertThat(metadata.getString(MetadataKeys.JOB_IDENTIFIER), is("job #1"));
        assertFalse(metadata.has(MetadataKeys.JOB_URL));
    }

    @Test
    public void updateVM_omitsJobUrlKeyWhenEmpty() throws Exception {
        AtomicReference<JSONObject> capturedBody = new AtomicReference<>();
        AnkaMgmtCommunicator communicator = capturingCommunicator(capturedBody);

        communicator.updateVM("vm-1", null, null, "job #1", "");

        JSONObject metadata = capturedBody.get().getJSONObject("metadata");
        assertThat(metadata.getString(MetadataKeys.JOB_IDENTIFIER), is("job #1"));
        assertFalse(metadata.has(MetadataKeys.JOB_URL));
    }

    @Test
    public void updateVM_doesNotSetExternalIdWhenNodeLinkIsNull() throws Exception {
        AtomicReference<JSONObject> capturedBody = new AtomicReference<>();
        AnkaMgmtCommunicator communicator = capturingCommunicator(capturedBody);

        communicator.updateVM("vm-1", null, null, "job #1", "https://jenkins.example/job/job/1/");

        assertFalse(capturedBody.get().has("external_id"));
        assertThat(capturedBody.get().opt("external_id"), nullValue());
    }

    @Test
    public void updateVM_setsExternalIdOnlyWhenNodeLinkProvided() throws Exception {
        AtomicReference<JSONObject> capturedBody = new AtomicReference<>();
        AnkaMgmtCommunicator communicator = capturingCommunicator(capturedBody);

        communicator.updateVM("vm-1", null, "https://jenkins.example/computer/agent-1/", null, null);

        assertTrue(capturedBody.get().has("external_id"));
        assertThat(capturedBody.get().getString("external_id"), is("https://jenkins.example/computer/agent-1/"));
        assertFalse(capturedBody.get().has("metadata"));
    }

    @Test
    public void updateVM_includesJobUrlOnlyWhenIdentifierMissing() throws Exception {
        AtomicReference<JSONObject> capturedBody = new AtomicReference<>();
        AnkaMgmtCommunicator communicator = capturingCommunicator(capturedBody);

        communicator.updateVM("vm-1", null, null, null, "https://jenkins.example/job/job/1/");

        JSONObject metadata = capturedBody.get().getJSONObject("metadata");
        assertFalse(metadata.has(MetadataKeys.JOB_IDENTIFIER));
        assertThat(metadata.getString(MetadataKeys.JOB_URL), is("https://jenkins.example/job/job/1/"));
    }

    @Test
    public void updateVM_omitsMetadataWhenBothJobFieldsEmpty() throws Exception {
        AtomicReference<JSONObject> capturedBody = new AtomicReference<>();
        AnkaMgmtCommunicator communicator = capturingCommunicator(capturedBody);

        communicator.updateVM("vm-1", "vm-name", null, "", "");

        JSONObject body = capturedBody.get();
        assertThat(body.getString("name"), is("vm-name"));
        assertFalse(body.has("metadata"));
    }

    @Test
    public void updateVM_usesPutOnVmIdPath() throws Exception {
        AtomicReference<AnkaMgmtCommunicator.RequestMethod> capturedMethod = new AtomicReference<>();
        AtomicReference<String> capturedPath = new AtomicReference<>();
        AnkaMgmtCommunicator communicator = new AnkaMgmtCommunicator("http://127.0.0.1:9") {
            @Override
            protected JSONObject doRequest(RequestMethod method, String path, JSONObject requestBody, int reqTimeout) {
                capturedMethod.set(method);
                capturedPath.set(path);
                JSONObject response = new JSONObject();
                response.put("status", "OK");
                return response;
            }
        };

        communicator.updateVM("abc-123", null, null, "job #1", "https://jenkins.example/job/job/1/");

        assertThat(capturedMethod.get(), is(AnkaMgmtCommunicator.RequestMethod.PUT));
        assertThat(capturedPath.get(), is("/api/v1/vm?id=abc-123"));
    }

    @Test
    public void metadataKeys_jobUrlConstant() {
        assertThat(MetadataKeys.JOB_URL, is("jenkins-job-url"));
        assertThat(MetadataKeys.JOB_IDENTIFIER, is("jenkins-job-id"));
    }

    private static AnkaMgmtCommunicator capturingCommunicator(AtomicReference<JSONObject> capturedBody) {
        return new AnkaMgmtCommunicator("http://127.0.0.1:9") {
            @Override
            protected JSONObject doRequest(RequestMethod method, String path, JSONObject requestBody, int reqTimeout) {
                capturedBody.set(requestBody);
                JSONObject response = new JSONObject();
                response.put("status", "OK");
                return response;
            }
        };
    }
}
