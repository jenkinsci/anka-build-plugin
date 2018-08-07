package com.veertu.plugin.anka;

import jenkins.model.Jenkins;
import jenkins.slaves.JnlpSlaveAgentProtocol;

public class JnlpCommandBuilder {

    static private String scriptTemplate =
            "echo \"Initiating startup script\" > log.txt\n" +
            "if [ ! -f agent.jar ]; then\n" +
            "   echo \"downloading jar\" >> log.txt\n" +
            "   CURL=$(curl --fail -O \"%s/jnlpJars/agent.jar\" --output agent.jar 2>&1)\n" +
            "   if [ $? -ne 0 ]; then\n" +
            "       CURL=$(curl --fail -O \"%s/jnlpJars/agent.jar\" --output agent.jar 2>&1)\n" +
            "       if [ $? -ne 0 ]; then\n" +
            "           echo $CURL | grep --quiet 'The requested URL returned error: 404' >> log.txt\n" +
            "           exit 1\n" +
            "       fi\n" +
            "   fi\n" +
            "fi\n" +
            "echo \"Executing java command: %s\" >> log.txt\n" +
            "%s &> log.txt\n";


    public static String makeCommand(String nodeName) {
        String effectiveJenkinsUrl = Jenkins.getInstance().getRootUrl();
        String format = "java -jar agent.jar -jnlpUrl %s/computer/%s/slave-agent.jnlp -secret %s";
        String secret = JnlpSlaveAgentProtocol.SLAVE_SECRET.mac(nodeName);
        return String.format(format, effectiveJenkinsUrl, nodeName, secret);
    }

    public static String makeStartUpScript(String nodeName) {

        String jarCommand = makeCommand(nodeName);
        String script = String.format(scriptTemplate, Jenkins.getInstance().getRootUrl(),
                Jenkins.getInstance().getRootUrl(), jarCommand, jarCommand);
        return script;

    }
}
