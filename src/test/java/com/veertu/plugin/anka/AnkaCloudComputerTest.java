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

    @Test
    public void shouldReturnNullWhenDisplayNameIsNull() {
        assertThat(AnkaCloudComputer.parseRunIdentityFromDisplayName(null) == null, is(true));
    }

    @Test
    public void shouldReturnNullWhenDisplayNameIsEmpty() {
        assertThat(AnkaCloudComputer.parseRunIdentityFromDisplayName("") == null, is(true));
    }

    @Test
    public void shouldReturnNullWhenBuildNumberIsNotNumeric() {
        assertThat(AnkaCloudComputer.parseRunIdentityFromDisplayName("folder/job-name #abc") == null, is(true));
    }

    @Test
    public void shouldReturnNullWhenBuildNumberIsZero() {
        assertThat(AnkaCloudComputer.parseRunIdentityFromDisplayName("folder/job-name #0") == null, is(true));
    }

    @Test
    public void shouldReturnNullWhenBuildNumberIsNegative() {
        assertThat(AnkaCloudComputer.parseRunIdentityFromDisplayName("folder/job-name #-1") == null, is(true));
    }

    @Test
    public void shouldReturnNullWhenNoSpaceBeforeHash() {
        // "name#42" lacks the Jenkins-standard " #" separator and must be rejected so
        // we do not misattribute outcomes to a spurious job name. This directly protects
        // the run-once cleanup from picking the wrong build.
        assertThat(AnkaCloudComputer.parseRunIdentityFromDisplayName("name#42") == null, is(true));
    }

    @Test
    public void shouldReturnNullWhenJobNameIsBlank() {
        assertThat(AnkaCloudComputer.parseRunIdentityFromDisplayName(" #42") == null, is(true));
    }

    @Test
    public void shouldParseIdentityWhenJobNameContainsNestedFolders() {
        AnkaCloudComputer.RunIdentity runIdentity =
                AnkaCloudComputer.parseRunIdentityFromDisplayName("top-folder/mid-folder/leaf-job #7");

        assertThat(runIdentity.getJobFullName(), is("top-folder/mid-folder/leaf-job"));
        assertThat(runIdentity.getBuildNumber(), is(7));
    }

    @Test
    public void shouldParseIdentityAndTrimTrailingWhitespace() {
        AnkaCloudComputer.RunIdentity runIdentity =
                AnkaCloudComputer.parseRunIdentityFromDisplayName("job-name #42   ");

        assertThat(runIdentity.getJobFullName(), is("job-name"));
        assertThat(runIdentity.getBuildNumber(), is(42));
    }

    @Test
    public void shouldParseIdentityUsingLastHashSeparator() {
        // Job names can legally contain '#', so we anchor on the last " #" to locate
        // the build number segment.
        AnkaCloudComputer.RunIdentity runIdentity =
                AnkaCloudComputer.parseRunIdentityFromDisplayName("weird #name #99");

        assertThat(runIdentity.getJobFullName(), is("weird #name"));
        assertThat(runIdentity.getBuildNumber(), is(99));
    }

    @Test
    public void shouldResolveAbortedResultToFailureOutcome() {
        assertThat(AnkaCloudComputer.resolveBuildOutcome(Result.ABORTED), is(AnkaCloudComputer.BuildOutcome.FAILURE));
    }

    @Test
    public void shouldResolveNotBuiltResultToFailureOutcome() {
        assertThat(AnkaCloudComputer.resolveBuildOutcome(Result.NOT_BUILT), is(AnkaCloudComputer.BuildOutcome.FAILURE));
    }

    @Test
    public void shouldNotMarkUnknownOutcomeAsKeepAliveError() {
        // UNKNOWN outcomes must not flip hadErrorsOnBuild, otherwise keepAliveOnError
        // could hold a run-once agent alive and let a pending task pick it up.
        assertThat(AnkaCloudComputer.shouldMarkBuildAsErrorForKeepAlive(AnkaCloudComputer.BuildOutcome.UNKNOWN), is(false));
    }
}
