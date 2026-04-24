package com.veertu.plugin.anka;

import hudson.model.ItemGroup;
import org.junit.Test;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.lang.reflect.Method;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class AnkaMgmtCloudDescriptorTest {

    @Test
    public void rootCaCredentialsDropdownRequiresPost() throws Exception {
        Method method = AnkaMgmtCloud.DescriptorImpl.class.getMethod(
                "doFillRootCaCredentialsIdItems",
                ItemGroup.class);

        assertThat(method.isAnnotationPresent(RequirePOST.class), is(true));
    }
}
