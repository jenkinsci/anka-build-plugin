package com.veertu.ankaMgmtSdk;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by asafgur on 18/05/2017.
 */
public class AnkaVmFactory {

    private static AnkaVmFactory ourInstance = new AnkaVmFactory();
    private Map<String, AnkaMgmtCommunicator> communicators;
    private static int vmCounter = 1;

    public static AnkaVmFactory getInstance() {
        return ourInstance;
    }
    private java.util.logging.Logger logger =  java.util.logging.Logger.getLogger("AnkaVmFactory");
    private AnkaVmFactory() {
        this.communicators = new HashMap<String, AnkaMgmtCommunicator>();
    }

    private AnkaMgmtCommunicator getCommunicator(String mgmtUrl) throws AnkaMgmtException {
        String communicatorKey = mgmtUrl;
        AnkaMgmtCommunicator communicator = this.communicators.get(communicatorKey);
        if (communicator == null) {
            communicator = new AnkaMgmtCommunicator(mgmtUrl);
            this.communicators.put(communicatorKey, communicator);
        }
        return communicator;
    }

    public AnkaMgmtVm makeAnkaVm(String mgmtUrl, String templateId,
                                 String tag, String nameTemplate, int sshPort) throws AnkaMgmtException {
        return makeAnkaVm(mgmtUrl, templateId, nameTemplate, tag, sshPort, null, null);
    }

    public AnkaMgmtVm makeAnkaVm(String mgmtUrl, String templateId,
                                 String tag, String nameTemplate, int sshPort, String startUpScript, String groupId) throws AnkaMgmtException {
        return makeAnkaVm(mgmtUrl, templateId, tag, nameTemplate, sshPort, startUpScript, groupId, -1);
    }

    public AnkaMgmtVm makeAnkaVm(String mgmtUrl, String templateId,
                                 String tag, String nameTemplate, int sshPort, String startUpScript, String groupId, int priority) throws AnkaMgmtException {

        logger.info(String.format("making anka vm, url: %s, " +
                "templateId: %s, sshPort: %d", mgmtUrl, templateId, sshPort));
        if (nameTemplate == null || nameTemplate.isEmpty())
            nameTemplate = "$template_name-$node_name-$ts";
        else if (!nameTemplate.contains("$ts"))
            nameTemplate = String.format("%s-%d", nameTemplate, vmCounter++);

        AnkaMgmtCommunicator communicator = getCommunicator(mgmtUrl);
        String sessionId = communicator.startVm(templateId, tag, nameTemplate, startUpScript, groupId, priority);
        AnkaMgmtVm vm = new ConcAnkaMgmtVm(sessionId, communicator, sshPort);
        return vm;

    }

    public List<AnkaVmTemplate> listTemplates(String mgmtUrl) throws AnkaMgmtException {
        AnkaMgmtCommunicator communicator = getCommunicator(mgmtUrl);
        return communicator.listTemplates();
    }

    public List<String> listTemplateTags(String mgmtUrl, String masterVmId) throws AnkaMgmtException {
        AnkaMgmtCommunicator communicator = getCommunicator(mgmtUrl);
        return communicator.getTemplateTags(masterVmId);
    }

    public List<NodeGroup> getNodeGroups(String mgmtUrl) throws AnkaMgmtException {
        AnkaMgmtCommunicator communicator = getCommunicator(mgmtUrl);
        return communicator.getNodeGroups();
    }
}
