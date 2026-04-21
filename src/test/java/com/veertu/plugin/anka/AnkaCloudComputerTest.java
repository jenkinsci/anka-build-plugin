package com.veertu.plugin.anka;

import hudson.model.Result;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class AnkaCloudComputerTest {

    @Test
    public void shouldResolveNullResultToUnknownOutcome() {
        assertThat(AnkaCloudComputer.resolveBuildOutcome(null), is(AnkaCloudComputer.BuildOutcome.UNKNOWN));
    }

    @Test
    public void shouldResolveSuccessResultToSuccessOutcome() {
        assertThat(AnkaCloudComputer.resolveBuildOutcome(Result.SUCCESS), is(AnkaCloudComputer.BuildOutcome.SUCCESS));
    }

    @Test
    public void shouldResolveFailureResultToFailureOutcome() {
        assertThat(AnkaCloudComputer.resolveBuildOutcome(Result.FAILURE), is(AnkaCloudComputer.BuildOutcome.FAILURE));
    }

    @Test
    public void shouldResolveUnstableResultToFailureOutcome() {
        assertThat(AnkaCloudComputer.resolveBuildOutcome(Result.UNSTABLE), is(AnkaCloudComputer.BuildOutcome.FAILURE));
    }

    @Test
    public void shouldMarkOnlyFailureOutcomeAsKeepAliveError() {
        assertThat(AnkaCloudComputer.shouldMarkBuildAsErrorForKeepAlive(AnkaCloudComputer.BuildOutcome.FAILURE), is(true));
        assertThat(AnkaCloudComputer.shouldMarkBuildAsErrorForKeepAlive(AnkaCloudComputer.BuildOutcome.UNKNOWN), is(false));
        assertThat(AnkaCloudComputer.shouldMarkBuildAsErrorForKeepAlive(AnkaCloudComputer.BuildOutcome.SUCCESS), is(false));
    }

    @Test
    public void shouldParseRunIdentityFromFullDisplayName() {
        AnkaCloudComputer.RunIdentity runIdentity =
                AnkaCloudComputer.parseRunIdentityFromDisplayName("folder/job-name #123");

        assertThat(runIdentity.getJobFullName(), is("folder/job-name"));
        assertThat(runIdentity.getBuildNumber(), is(123));
    }

    @Test
    public void shouldReturnNullWhenDisplayNameHasNoBuildNumber() {
        AnkaCloudComputer.RunIdentity runIdentity =
                AnkaCloudComputer.parseRunIdentityFromDisplayName("folder/job-name");

        assertThat(runIdentity == null, is(true));
    }
}
