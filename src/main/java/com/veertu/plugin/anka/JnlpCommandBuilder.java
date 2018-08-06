package com.veertu.plugin.anka;

import jenkins.model.Jenkins;
import jenkins.slaves.JnlpSlaveAgentProtocol;

public class JnlpCommandBuilder {

    public static String makeCommand(String nodeName) {
        String effectiveJenkinsUrl = Jenkins.getInstance().getRootUrl();
        String format = "java -jar agent.jar -jnlpUrl %s/computer/%s/slave-agent.jnlp -secret %s";
        String secret = JnlpSlaveAgentProtocol.SLAVE_SECRET.mac(nodeName);
        return String.format(format, effectiveJenkinsUrl, nodeName, secret);
    }

    public static String makeStartUpScript(String nodeName) {
        String scriptTemplate = "echo \"Initiating startup script\" > log.txt\n" +
                "if [ ! -f agent.jar ]; then\n" +
                "\techo \"downloading jar\" >> log.txt\n" +
                "\tCURL=$(curl --fail -O \"%s/jnlpJars/agent.jar\" --output agent.jar 2>&1)\n" +
                "\tif [ $? -ne 0 ]; then\n" +
                "\t\tCURL=$(curl --fail -O \"%s/jnlpJars/agent.jar\" --output agent.jar 2>&1)\n" +
                "\t\tif [ $? -ne 0 ]; then\n" +
                "        \t\techo $CURL | grep --quiet 'The requested URL returned error: 404' >> log.txt\n" +
                "\t\t\texit 1\n" +
                "        \tfi\n" +
                "\tfi\n" +
                "fi\n";
        String jarCommand = makeCommand(nodeName);
        String script = String.format(scriptTemplate, Jenkins.getInstance().getRootUrl(), Jenkins.getInstance().getRootUrl());
        return script + jarCommand + ">> log.txt";

    }
}
