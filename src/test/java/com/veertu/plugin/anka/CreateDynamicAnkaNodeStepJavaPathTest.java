package com.veertu.plugin.anka;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class CreateDynamicAnkaNodeStepJavaPathTest {

    @Test
    public void javaPathDefaultsToNull() {
        CreateDynamicAnkaNodeStep step = new CreateDynamicAnkaNodeStep("master-vm");
        assertThat(step.getJavaPath(), is(nullValue()));
    }

    @Test
    public void setJavaPathDelegatesToDynamicTemplate() {
        CreateDynamicAnkaNodeStep step = new CreateDynamicAnkaNodeStep("master-vm");
        step.setJavaPath("/usr/libexec/java_home/bin/java");

        assertThat(step.getJavaPath(), is("/usr/libexec/java_home/bin/java"));
        assertThat(step.getDynamicSlaveTemplate().getJavaPath(), is("/usr/libexec/java_home/bin/java"));
    }

    @Test
    public void emptyJavaPathBecomesNull() {
        CreateDynamicAnkaNodeStep step = new CreateDynamicAnkaNodeStep("master-vm");
        step.setJavaPath("");

        assertThat(step.getJavaPath(), is(nullValue()));
    }
}
