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

    public static AnkaVmFactory getInstance() {
        return ourInstance;
    }
    private java.util.logging.Logger logger =  java.util.logging.Logger.getLogger("AnkaVmFactory");
    private AnkaVmFactory() {
        this.communicators = new HashMap<String, AnkaMgmtCommunicator>();
    }

    private AnkaMgmtCommunicator getCommunicator(String mgmtHost, String mgmtPort) throws AnkaMgmtException {
        String communicatorKey = mgmtHost + ":" + mgmtPort;
        AnkaMgmtCommunicator communicator = this.communicators.get(communicatorKey);
        if (communicator == null) {
            communicator = new AnkaMgmtCommunicator(mgmtHost, mgmtPort);
            this.communicators.put(communicatorKey, communicator);
        }
        return communicator;
    }

    public AnkaMgmtVm makeAnkaVm(String mgmtHost, String mgmtPort, String templateId, String tag, int sshPort) throws AnkaMgmtException {
        logger.info(String.format("making anka vm, host: %s, port: %s, " +
                "templateId: %s, sshPort: %d", mgmtHost, mgmtPort, templateId, sshPort));
        AnkaMgmtCommunicator communicator = getCommunicator(mgmtHost, mgmtPort);
        String sessionId = communicator.startVm(templateId, tag);
        AnkaMgmtVm vm = new ConcAnkaMgmtVm(sessionId, communicator, sshPort);
        return vm;

    }

    public List<AnkaVmTemplate> listTemplates(String mgmtHost, String mgmtPort) throws AnkaMgmtException {
        AnkaMgmtCommunicator communicator = getCommunicator(mgmtHost, mgmtPort);
        return communicator.listTemplates();
    }

    public List<String> listTemplateTags(String mgmtHost, String ankaMgmtPort, String masterVmId) throws AnkaMgmtException {
        AnkaMgmtCommunicator communicator = getCommunicator(mgmtHost, ankaMgmtPort);
        return communicator.getTemplateTags(masterVmId);
    }
}