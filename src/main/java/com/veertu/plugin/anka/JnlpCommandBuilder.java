package com.veertu.plugin.anka;

import jenkins.model.Jenkins;
import jenkins.slaves.JnlpSlaveAgentProtocol;

public class JnlpCommandBuilder {

    static private String scriptTemplate =
            "echo \"Initiating startup script\" > /tmp/log.txt\n" +
            "if [ ! -f agent.jar ]; then\n" +
            "   echo \"downloading jar from %s\" >> /tmp/log.txt\n" +
            "   curl --fail -s %s/jnlpJars/agent.jar -o agent.jar\n" +
            "   if [ $? -ne 0 ]; then\n" +
            "       curl --fail -s %s/jnlpJars/slave.jar -o agent.jar\n" +
            "       if [ $? -ne 0 ]; then\n" +
            "           echo 'Error downloading agent jar' >> /tmp/log.txt\n" +
            "           exit 1\n" +
            "       fi\n" +
            "   fi\n" +
            "fi\n" +
            "echo \"Executing java command: %s\" >> /tmp/log.txt\n" +
            "%s &> /tmp/log.txt\n";


    public static String makeCommand(String nodeName) {
        String effectiveJenkinsUrl = Jenkins.getInstance().getRootUrl();
        String format = "java -jar agent.jar -jnlpUrl %s/computer/%s/slave-agent.jnlp -secret %s";
        String secret = JnlpSlaveAgentProtocol.SLAVE_SECRET.mac(nodeName);
        return String.format(format, effectiveJenkinsUrl, nodeName, secret);
    }

    public static String makeStartUpScript(String nodeName) {

        String jarCommand = makeCommand(nodeName);
        String jenkinsUrl = Jenkins.getInstance().getRootUrl();
        String script = String.format(scriptTemplate, jenkinsUrl, jenkinsUrl, jenkinsUrl, jarCommand, jarCommand);
        return script;

    }
}
