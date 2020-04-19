package com.veertu.ankaMgmtSdk;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by asafgur on 18/05/2017.
 */

public class AnkaAPI {

    private AnkaMgmtCommunicator communicator;
    private static int vmCounter = 1;

    private static transient java.util.logging.Logger logger =  java.util.logging.Logger.getLogger("AnkaAPI");

    public AnkaAPI(List<String> mgmtURLS, boolean skipTLSVerification, String rootCA) {
        this.communicator = new AnkaMgmtCommunicator(mgmtURLS, skipTLSVerification, rootCA);
    }

    public AnkaAPI(List<String> mgmtURLS, boolean skipTLSVerification, String client, String key, AuthType authType, String rootCA) {
        switch (authType) {
            case CERTIFICATE:
                this.communicator = new AnkaMgmtClientCertAuthCommunicator(mgmtURLS, skipTLSVerification, client, key, rootCA);
                break;
            case OPENID_CONNECT:
                this.communicator = new AnkaMgmtOpenIdCommunicator(mgmtURLS, skipTLSVerification, client, key, rootCA);
                break;
        }
    }

    public AnkaAPI(String mgmtUrl, boolean skipTLSVerification, String rootCA) {
        this.communicator = new AnkaMgmtCommunicator(mgmtUrl, skipTLSVerification, rootCA);
    }

    public AnkaAPI(String mgmtUrl, boolean skipTLSVerification, String client, String key, AuthType authType, String rootCA) {
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
                                 String tag, String nameTemplate, int sshPort, String startUpScript, String groupId,
                                 int priority, String name, String externalId) throws AnkaMgmtException {

        logger.info(String.format("making anka vm," +
                "templateId: %s, sshPort: %d", templateId, sshPort));
        if (nameTemplate == null || nameTemplate.isEmpty())
            nameTemplate = "$template_name-$node_name-$ts";
        else if (!nameTemplate.contains("$ts"))
            nameTemplate = String.format("%s-%d", nameTemplate, vmCounter++);

        String sessionId = communicator.startVm(templateId, tag, nameTemplate, startUpScript, groupId, priority, name, externalId);
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

    public void revertLatestTag(String templateID) throws AnkaMgmtException {
        communicator.revertRegistryVM(templateID);
    }

    public List<JSONObject> getImageRequests() throws AnkaMgmtException {
        return communicator.getImageRequests();

    }

    public String getSaveImageStatus(String reqId) throws AnkaMgmtException {
        return communicator.getSaveImageStatus(reqId);
    }

    public AnkaCloudStatus getStatus() throws AnkaMgmtException {
        return communicator.status();
    }

    public boolean terminateInstance(String vmId) throws AnkaMgmtException {
        return communicator.terminateVm(vmId);
    }

    public AnkaVmInstance showInstance(String vmId) throws AnkaMgmtException {
        return communicator.showVm(vmId);
    }

    public void updateInstance(AnkaMgmtVm vm, String name, String jenkinsNodeLink, String jobIdentifier) throws AnkaMgmtException {
        communicator.updateVM(vm.getId(), name, jenkinsNodeLink, jobIdentifier);
    }
}
