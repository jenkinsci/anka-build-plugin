package com.veertu.ankaMgmtSdk;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by asafgur on 18/05/2017.
 */

public class AnkaAPI {

    private AnkaMgmtCommunicator communicator;

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

    public String startVM(String templateId, String tag, String nameTemplate, String startUpScript, String groupId, int priority, String name, String externalId) throws AnkaMgmtException {
        return communicator.startVm(templateId, tag, nameTemplate, startUpScript, groupId, priority, name, externalId);
    }

    public String startVM(String templateId, String tag, String startUpScript, String groupId, int priority, String name, String externalId) throws AnkaMgmtException {
        return communicator.startVm(templateId, tag, "$template_name-$node_name-$ts", startUpScript, groupId, priority, name, externalId);
    }

    public boolean terminateInstance(String vmId) throws AnkaMgmtException {
        return communicator.terminateVm(vmId);
    }

    public AnkaVmInstance showInstance(String vmId) throws AnkaMgmtException {
        return communicator.showVm(vmId);
    }

    public void updateInstance(String vmId, String name, String jenkinsNodeLink, String jobIdentifier) throws AnkaMgmtException {
        communicator.updateVM(vmId, name, jenkinsNodeLink, jobIdentifier);
    }

    public String saveImage(String instanceId, String targetVMId, String tagToPush,
                            String description, Boolean suspend,
                            String shutdownScript, boolean deleteLatest, String latestTag,
                            boolean doSuspendTest) throws AnkaMgmtException {
        return communicator.saveImage(instanceId, targetVMId, null, tagToPush, description, suspend,
                shutdownScript, deleteLatest, latestTag, doSuspendTest);
    }
}
