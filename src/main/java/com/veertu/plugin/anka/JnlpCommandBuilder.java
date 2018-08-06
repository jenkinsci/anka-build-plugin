package com.veertu.plugin.anka;

import jenkins.model.Jenkins;
import jenkins.slaves.JnlpSlaveAgentProtocol;

public class JnlpCommandBuilder {

    public static String makeCommand(String nodeName, AnkaCloudSlaveTemplate template) {
        String effectiveJenkinsUrl = Jenkins.getInstance().getRootUrl();
        String format = "java -jar agent.jar -jnlpUrl %s/computer/%s/slave-agent.jnlp -secret %s";
        String secret = JnlpSlaveAgentProtocol.SLAVE_SECRET.mac(nodeName);
        return String.format(format, effectiveJenkinsUrl, nodeName, secret);
    }
}
