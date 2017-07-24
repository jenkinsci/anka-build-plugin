package com.veertu.ankaMgmtSdk;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;

import java.util.List;

/**
 * Created by asafgur on 18/05/2017.
 */
public class AnkaVmFactory {

    private static AnkaVmFactory ourInstance = new AnkaVmFactory();
    private AnkaMgmtCommunicator communicator;

    public static AnkaVmFactory getInstance() {
        return ourInstance;
    }
    private java.util.logging.Logger logger =  java.util.logging.Logger.getLogger("AnkaVmFactory");
    private AnkaVmFactory() {
    }

    private AnkaMgmtCommunicator getCommunicator(String mgmtHost, String mgmtPort) throws AnkaMgmtException {
        if (this.communicator == null) {
            this.communicator = new AnkaMgmtCommunicator(mgmtHost, mgmtPort);
        }
        return this.communicator;
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