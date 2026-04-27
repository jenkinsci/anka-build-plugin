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
            "nohup %s &> /tmp/log1.txt & \n";


    public static String makeCommand(String nodeName, String extraArgs, String javaArgs, String overrideJenkinsUrl) {
        return makeCommand(nodeName, extraArgs, javaArgs, overrideJenkinsUrl, null);
    }

    public static String makeCommand(String nodeName, String extraArgs, String javaArgs, String overrideJenkinsUrl, String jnlpTunnel) {
        String format = "java %s -jar agent.jar -url %s -secret %s -name %s ";
        String secret = JnlpSlaveAgentProtocol.SLAVE_SECRET.mac(nodeName);
        if (javaArgs == null) {
            javaArgs = "";
        }

        String effectiveJenkinsUrl = Jenkins.get().getRootUrl();

        if (overrideJenkinsUrl != null && !overrideJenkinsUrl.isEmpty()) {
            effectiveJenkinsUrl = overrideJenkinsUrl;
        }

        String command = String.format(format, javaArgs, effectiveJenkinsUrl, secret, nodeName);
        if (jnlpTunnel != null) {
            command += "-tunnel " + jnlpTunnel + " ";
        }
        if (extraArgs != null) {
            command += extraArgs;
        }
        return command;
    }

    public static String makeStartUpScript(String nodeName, String extraArgs, String javaArgs, String overrideJenkinsUrl) {
        return makeStartUpScript(nodeName, extraArgs, javaArgs, overrideJenkinsUrl, null);
    }

    public static String makeStartUpScript(String nodeName, String extraArgs, String javaArgs, String overrideJenkinsUrl, String jnlpTunnel) {

        String jarCommand = makeCommand(nodeName, extraArgs, javaArgs, overrideJenkinsUrl, jnlpTunnel);
        String jenkinsUrl = Jenkins.get().getRootUrl();
        String script = String.format(scriptTemplate, jenkinsUrl, jenkinsUrl, jenkinsUrl, jarCommand, jarCommand);
        return script;

    }
}
