package com.veertu.ankaMgmtSdk;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;

import java.util.List;

/**
 * Created by asafgur on 18/05/2017.
 */

public class AnkaAPI {

    private final String mgmtURL;
    private AnkaMgmtCommunicator communicator;
    private static int vmCounter = 1;

    private transient java.util.logging.Logger logger =  java.util.logging.Logger.getLogger("AnkaAPI");

    public AnkaAPI(String mgmtUrl, boolean skipTLSVerification, String rootCA) {
        this.mgmtURL = mgmtUrl;
        this.communicator = new AnkaMgmtCommunicator(mgmtUrl, skipTLSVerification, rootCA);
    }

    public AnkaAPI(String mgmtUrl, boolean skipTLSVerification, String client, String key, AuthType authType, String rootCA) {
        this.mgmtURL = mgmtUrl;

        switch (authType) {
            case CERTIFICATE:
                this.communicator = new AnkaMgmtClientCertAuthCommunicator(mgmtUrl, skipTLSVerification, client, key, rootCA);
                break;
            case OPENID_CONNECT:
                this.communicator = new AnkaMgmtOpenIdCommunicator(mgmtUrl, skipTLSVerification, client, key, rootCA);
                break;
        }
    }


    public AnkaMgmtVm makeAnkaVm(String templateId,
                                 String tag, String nameTemplate, int sshPort, String startUpScript, String groupId, int priority) throws AnkaMgmtException {

        logger.info(String.format("making anka vm," +
                "templateId: %s, sshPort: %d", templateId, sshPort));
        if (nameTemplate == null || nameTemplate.isEmpty())
            nameTemplate = "$template_name-$node_name-$ts";
        else if (!nameTemplate.contains("$ts"))
            nameTemplate = String.format("%s-%d", nameTemplate, vmCounter++);

        String sessionId = communicator.startVm(templateId, tag, nameTemplate, startUpScript, groupId, priority);
        AnkaMgmtVm vm = new ConcAnkaMgmtVm(sessionId, communicator, sshPort);
        return vm;

    }

    public List<AnkaVmTemplate> listTemplates() throws AnkaMgmtException {
        return communicator.listTemplates();
    }

    public List<String> listTemplateTags(String masterVmId) throws AnkaMgmtException {
        return communicator.getTemplateTags(masterVmId);
    }

    public List<NodeGroup> getNodeGroups() throws AnkaMgmtException {
        return communicator.getNodeGroups();
    }
}
