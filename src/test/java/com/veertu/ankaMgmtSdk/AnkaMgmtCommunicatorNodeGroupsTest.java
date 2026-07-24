package com.veertu.ankaMgmtSdk;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AnkaMgmtCommunicatorNodeGroupsTest {

    @Test
    public void getNodeGroups_malformedBody_throwsAnkaMgmtException() {
        AnkaMgmtCommunicator communicator = communicatorReturning(response -> {
            response.put("status", "OK");
            response.put("body", "not-an-array");
        });

        AnkaMgmtException thrown = assertThrows(AnkaMgmtException.class, communicator::getNodeGroups);
        assertThat(thrown.getCause() instanceof org.json.JSONException, is(true));
    }

    @Test
    public void getNodeGroups_okResponse_returnsParsedGroups() throws Exception {
        AnkaMgmtCommunicator communicator = communicatorReturning(response -> {
            response.put("status", "OK");
            JSONArray body = new JSONArray();
            JSONObject group = new JSONObject();
            group.put("id", "group-uuid");
            group.put("name", "onprem");
            group.put("description", "");
            body.put(group);
            response.put("body", body);
        });

        List<NodeGroup> groups = communicator.getNodeGroups();
        assertThat(groups, hasSize(1));
        assertThat(groups.get(0).getId(), is("group-uuid"));
        assertThat(groups.get(0).getName(), is("onprem"));
    }

    private static AnkaMgmtCommunicator communicatorReturning(ResponseBuilder builder) {
        return new AnkaMgmtCommunicator("http://127.0.0.1:9") {
            @Override
            protected JSONObject doRequest(RequestMethod method, String path, JSONObject requestBody, int reqTimeout) {
                JSONObject response = new JSONObject();
                builder.build(response);
                return response;
            }
        };
    }

    @FunctionalInterface
    private interface ResponseBuilder {
        void build(JSONObject response);
    }
}
