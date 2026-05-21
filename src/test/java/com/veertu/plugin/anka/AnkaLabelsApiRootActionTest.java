package com.veertu.plugin.anka;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class AnkaLabelsApiRootActionTest {

    @Test
    public void readBoundedRequestBody_acceptsBodyAtLimit() throws Exception {
        byte[] body = "{\"mode\":\"replace\",\"templates\":[]}".getBytes(StandardCharsets.UTF_8);

        String text = AnkaLabelsApiRootAction.readBoundedRequestBody(body.length, new ByteArrayInputStream(body), body.length);

        assertThat(text, is("{\"mode\":\"replace\",\"templates\":[]}"));
    }

    @Test(expected = AnkaLabelsApiRootAction.RequestBodyTooLargeException.class)
    public void readBoundedRequestBody_rejectsBodyOverLimit() throws Exception {
        byte[] body = "0123456789".getBytes(StandardCharsets.UTF_8);

        AnkaLabelsApiRootAction.readBoundedRequestBody(body.length, new ByteArrayInputStream(body), body.length - 1);
    }

    @Test(expected = AnkaLabelsApiRootAction.RequestBodyTooLargeException.class)
    public void readBoundedRequestBody_rejectsContentLengthOverLimitWithoutReading() throws Exception {
        AnkaLabelsApiRootAction.readBoundedRequestBody(100L, new ByteArrayInputStream(new byte[0]), 10);
    }

    @Test
    public void cloudNameFromLabelsRequest_decodesCloudNameFromPathInfo() {
        assertThat(
                AnkaLabelsApiRootAction.cloudNameFromLabelsRequest("/anka-build-cloud/labels/Anka%20Build%20Cloud"),
                is("Anka Build Cloud"));
        assertThat(AnkaLabelsApiRootAction.cloudNameFromLabelsRequest("/anka-build-cloud/labels/test-anka-cloud/extra"), is("test-anka-cloud"));
        assertThat(AnkaLabelsApiRootAction.cloudNameFromLabelsRequest("/anka-build-cloud/labels/"), is(nullValue()));
        assertThat(AnkaLabelsApiRootAction.cloudNameFromLabelsRequest("/other/path"), is(nullValue()));
    }
}
