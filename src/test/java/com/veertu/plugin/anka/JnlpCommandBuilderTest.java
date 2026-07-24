package com.veertu.plugin.anka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

@WithJenkins
public class JnlpCommandBuilderTest {

    private JenkinsRule jenkins;

    @BeforeEach
    void setUp(JenkinsRule jenkins) {
        this.jenkins = jenkins;
    }

    @Test
    public void shouldUseModernAgentJarArguments() {
        String command = JnlpCommandBuilder.makeCommand("anka-agent-1", "--verbose", "-Xmx512m", "http://jenkins.example/");

        assertThat(command, containsString("java -Xmx512m -jar agent.jar"));
        assertThat(command, containsString("-url http://jenkins.example/"));
        assertThat(command, containsString("-name anka-agent-1"));
        assertThat(command, containsString("-secret "));
        assertThat(command, containsString("--verbose"));
        assertThat(command, not(containsString("-jnlpUrl")));
        assertThat(command, not(containsString("slave-agent.jnlp")));
        assertThat(command, not(containsString("jenkinsagent.jnlp")));
    }

    @Test
    public void fiveArgMakeCommandDefaultsJavaPathToBareJava() {
        String command = JnlpCommandBuilder.makeCommand(
                "anka-agent-1", null, "-Xmx256m", "http://jenkins.example/", "tunnel.example:50000");

        assertThat(command, startsWith("java -Xmx256m -jar agent.jar"));
        assertThat(command, containsString("-tunnel tunnel.example:50000"));
    }

    @Test
    public void fourArgMakeStartUpScriptUsesBareJava() {
        String script = JnlpCommandBuilder.makeStartUpScript(
                "anka-agent-1", null, "-Xmx256m", "http://jenkins.example/");

        assertThat(script, containsString("java -Xmx256m -jar agent.jar"));
        assertThat(script, containsString("curl --fail -s "));
    }

    @Test
    public void fiveArgMakeStartUpScriptIncludesTunnelAndBareJava() {
        String script = JnlpCommandBuilder.makeStartUpScript(
                "anka-agent-1", null, "-Xmx256m", "http://jenkins.example/", "tunnel.example:50000");

        assertThat(script, containsString("java -Xmx256m -jar agent.jar"));
        assertThat(script, containsString("-tunnel tunnel.example:50000"));
    }

    @Test
    public void shouldIncludeTunnelWhenConfigured() {
        AnkaCloudSlaveTemplate template = new AnkaCloudSlaveTemplate();
        template.setLaunchMethod(LaunchMethod.JNLP);
        template.setJnlpJenkinsOverrideUrl("http://jenkins.example/");
        template.setJnlpTunnel("tunnel.example:50000");

        String command = AnkaOnDemandSlave.createStartUpScript(template, "anka-agent-1");

        assertThat(command, containsString("-tunnel tunnel.example:50000"));
    }

    @Test
    public void shouldUseBareJavaWhenJavaPathUnset() {
        String command = JnlpCommandBuilder.makeCommand(
                "anka-agent-1", null, "-Xmx256m", "http://jenkins.example/", null, null);

        assertThat(command, startsWith("java -Xmx256m -jar agent.jar"));
    }

    @Test
    public void shouldUseBareJavaWhenJavaPathBlank() {
        String command = JnlpCommandBuilder.makeCommand(
                "anka-agent-1", null, "", "http://jenkins.example/", null, "");

        assertThat(command, startsWith("java  -jar agent.jar"));
    }

    @Test
    public void shouldUseConfiguredJavaPath() {
        String command = JnlpCommandBuilder.makeCommand(
                "anka-agent-1",
                null,
                "-Xmx256m",
                "http://jenkins.example/",
                null,
                "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java");

        assertThat(command, startsWith("/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java -Xmx256m -jar agent.jar"));
    }

    @Test
    public void createStartUpScriptUsesTemplateJavaPath() {
        AnkaCloudSlaveTemplate template = new AnkaCloudSlaveTemplate();
        template.setLaunchMethod(LaunchMethod.JNLP);
        template.setJnlpJenkinsOverrideUrl("http://jenkins.example/");
        template.setJavaPath("/opt/homebrew/opt/openjdk@21/bin/java");

        String script = AnkaOnDemandSlave.createStartUpScript(template, "anka-agent-1");

        assertThat(script, containsString("/opt/homebrew/opt/openjdk@21/bin/java "));
        assertThat(script, containsString("-jar agent.jar"));
    }
}
