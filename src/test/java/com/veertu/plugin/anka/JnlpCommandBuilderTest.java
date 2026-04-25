package com.veertu.plugin.anka;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class JnlpCommandBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

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
    public void shouldIncludeTunnelWhenConfigured() {
        AnkaCloudSlaveTemplate template = new AnkaCloudSlaveTemplate();
        template.setLaunchMethod(LaunchMethod.JNLP);
        template.setJnlpJenkinsOverrideUrl("http://jenkins.example/");
        template.setJnlpTunnel("tunnel.example:50000");

        String command = AnkaOnDemandSlave.createStartUpScript(template, "anka-agent-1");

        assertThat(command, containsString("-tunnel tunnel.example:50000"));
    }
}
