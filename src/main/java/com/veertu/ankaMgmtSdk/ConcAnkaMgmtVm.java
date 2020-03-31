package com.veertu.ankaMgmtSdk;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.ankaMgmtSdk.exceptions.VMDoesNotExistException;
import com.veertu.ankaMgmtSdk.exceptions.VmAlreadyTerminatedException;

import java.io.IOException;

/**
 * Created by asafgur on 17/05/2017.
 */
public class ConcAnkaMgmtVm implements AnkaMgmtVm {

    private final AnkaMgmtCommunicator communicator;
    private final String sessionId;
    private final long waitUnit = 1000;
    private final long maxRunningTimeout = waitUnit * 60 * 60; // 1 hour
    private final long maxIpTimeout = waitUnit * 240;
    private final int sshConnectionPort;
    private AnkaVmSession cachedVmSession;
    private final int cacheTime = 5; // 5 seconds
    private int lastCached = 0;
    private static transient java.util.logging.Logger logger = java.util.logging.Logger.getLogger("anka-sdk");


    public ConcAnkaMgmtVm(String sessionId, AnkaMgmtCommunicator communicator, int sshConnectionPort) {
        this.communicator = communicator;
        this.sessionId = sessionId;
        this.sshConnectionPort = sshConnectionPort;
        logger.info(String.format("init VM %s", sessionId));
    }

    public String getStatus() throws AnkaMgmtException {
        AnkaVmSession session = this.communicator.showVm(this.sessionId);
        if (session == null) {
            throw new VMDoesNotExistException(this.sessionId);
        }
        return session.getSessionState();
    }

    private AnkaVmSession getSession() throws AnkaMgmtException {
        return this.communicator.showVm(this.sessionId);
    }

    private String getIp() throws AnkaMgmtException {
        AnkaVmSession session = this.communicator.showVm(this.sessionId);
        if ( session.getVmInfo() == null) {
            return null;
        }
        String ip = session.getVmInfo().getVmIp();
        if (ip != null && !ip.equals("")) {
            return ip;
        }
        return null;
    }

    private int unixTime() {
        return (int) (System.currentTimeMillis() / 1000L);
    }

    private boolean shouldInvalidate() {
        int timeNow = unixTime();
        if (timeNow - this.lastCached > this.cacheTime) {
            this.lastCached = timeNow;
            return true;
        }
        return false;
    }

    private AnkaVmSession getSessionInfoCache() {
        try {
            if (this.cachedVmSession == null || this.shouldInvalidate()) {
                AnkaVmSession session = this.communicator.showVm(this.sessionId);
                if (session != null) {
                    this.cachedVmSession = session;
                } else {
                    logger.info("info for vm is null");
                }
            }
            return this.cachedVmSession;
        } catch (AnkaMgmtException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isPulling() throws AnkaMgmtException {
        String status = getStatus();
        if (status.equals("Pulling")) {
            return true;
        }
        return false;
    }


    private boolean isStarting() throws AnkaMgmtException {
        AnkaVmSession sessionInfoCache = getSessionInfoCache();
        if (sessionInfoCache == null) {
            return true;
        }
        AnkaVmSession session = getSession();
        String status = session.getSessionState();


        switch (status){
            case "Scheduling":
            case "Pulling":
                return true;
            case "Started":
                return false;
            case "Terminated":
            case "Terminating":
            case "Stopping":
            case "Stopped":
            case "Error":
                String message = session.getMessage();
                if (message != null) {
                    throw new AnkaMgmtException(String.format("Unexpected state %s for vm %s, Message: %s", status, sessionId, message));
                }
                throw new AnkaMgmtException(String.format("Unexpected state %s for vm %s", status, sessionId));
            default:
                return true;
        }
    }

    private boolean isScheduling() throws AnkaMgmtException {
        AnkaVmSession sessionInfoCache = getSessionInfoCache();
        if (sessionInfoCache == null) {
            return true;
        }
        AnkaVmSession session = getSession();
        String status = session.getSessionState();


        switch (status){
            case "Scheduling":
                return true;
            case "Pulling":
            case "Started":
                return false;
            case "Terminated":
            case "Terminating":
            case "Stopping":
            case "Stopped":
            case "Error":
                String message = session.getMessage();
                if (message != null) {
                    throw new AnkaMgmtException(String.format("Unexpected state %s for vm %s, Message: %s", status, sessionId, message));
                }
                throw new AnkaMgmtException(String.format("Unexpected state %s for vm %s", status, sessionId));
            default:
                return true;
        }
    }

    public String waitForBoot(int schedulingTimeout) throws InterruptedException, IOException, AnkaMgmtException {
        logger.info(String.format("waiting for vm %s to boot", this.sessionId));
        int timeWaited = 0;

        while (isScheduling()) {
            // terminate if scheduling timeout is reached
            Thread.sleep(waitUnit);
            timeWaited += waitUnit;
            logger.info(String.format("waiting for vm %s %d to stop scheduling", this.sessionId, timeWaited));
            if (timeWaited > schedulingTimeout * waitUnit) {
                logger.info(String.format("Timed out while waiting for %s to stop scheduling. Terminating VM", this.sessionId));
                this.terminate();
                throw new IOException("vm scheduling too long");
            }
        }

        timeWaited = 0;

        while (isStarting()) {
            // wait for the vm to spin up
            try {
                Thread.sleep(waitUnit);
                timeWaited += waitUnit;
                logger.info(String.format("waiting for vm %s %d to boot", this.sessionId, timeWaited));
                if (timeWaited > maxRunningTimeout) {
                    this.terminate();
                    throw new IOException("could not start vm");

                }
            } catch (InterruptedException e) {
                if (isPulling()) {  // Don't let jenkins interrupt us while we are pulling
                    logger.info(String.format("vm %s is pulling, ignoring InterruptedException", this.sessionId));
                } else {
                    throw e;
                }
            }
        }

        String ip;
        timeWaited = 0;
        logger.info(String.format("waiting for vm %s to get an ip ", this.sessionId));
        while (true) { // wait to get machine ip

            ip = this.getIp();
            if (ip != null)
                break;

            Thread.sleep(waitUnit);
            timeWaited += waitUnit;
            logger.info(String.format("waiting for vm %s %d to get ip ", this.sessionId, timeWaited));
            if (timeWaited > maxIpTimeout) {
                this.terminate();
                throw new IOException("VM started but couldn't acquire ip");
            }
        }
        // now that we have a running vm we should be able to create a launcher
        return ip;
    }


    public String getId() {
        return sessionId;
    }

    public String getName() {
        AnkaVmSession session = this.getSessionInfoCache();
        if (session == null) {
            return "";
        }
        AnkaVmInfo vmInfo = session.getVmInfo();
        if (vmInfo == null) {
            return "";
        }
        return session.getVmInfo().getName();
    }

    public String getConnectionIp() {
        AnkaVmSession session = this.getSessionInfoCache();
        if (session == null || session.getVmInfo() == null)
            return null;

        return session.getVmInfo().getHostIp();
    }

    public int getConnectionPort() {
        AnkaVmSession session = this.getSessionInfoCache();
        if (session == null){
            return 0;
        }
        AnkaVmInfo vmInfo = session.getVmInfo();
        if (vmInfo == null) {
            return 0;
        }
        for (PortForwardingRule rule: vmInfo.getPortForwardingRules()) {
            if (rule.getGuestPort() == this.sshConnectionPort) {
                return rule.getHostPort();
            }
        }
        return 0;
    }

    public void terminate() throws AnkaMgmtException {
        try {
            this.communicator.terminateVm(this.sessionId);
        } catch (VmAlreadyTerminatedException e) {
            logger.info(String.format("VM %s already terminated", this.sessionId));
        }
    }

    public String saveImage(String targetVMId, String tag, String description, Boolean suspend, String shutdownScript,
                          Boolean revertBeforePush,
                          String revertTag,
                          Boolean doSuspendTest) throws AnkaMgmtException {
        return this.communicator.saveImage(this.getId(), targetVMId, null, tag, description, suspend,
                shutdownScript, revertBeforePush, revertTag, doSuspendTest);
    }


    public boolean isRunning() {
        AnkaVmSession session = this.getSessionInfoCache();
        if (session == null ) {
            return false;
        }
        return session.getSessionState().equals("Started") && session.getVmInfo().getStatus().equals("running");
    }

    public String getInfo() {
        AnkaVmSession session = this.getSessionInfoCache();
        if (session == null) {
            return "";
        }
        return String.format("host: %s, uuid: %s, machine ip: %s",
                session.getVmInfo().getHostIp(), session.getVmInfo().getUuid(), session.getVmInfo().getVmIp());
    }
}
