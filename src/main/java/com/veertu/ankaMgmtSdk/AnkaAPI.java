package com.veertu.ankaMgmtSdk;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.plugin.anka.RunOnceCloudRetentionStrategy;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Created by asafgur on 18/05/2017.
 */

public class AnkaAPI {

    private static final transient Logger LOGGER = Logger.getLogger(RunOnceCloudRetentionStrategy.class.getName());

    private AnkaMgmtCommunicator communicator;
    private transient Map<String,AnkaVmInstance> instances;
    private transient long instancesCacheTime = 7;
    private transient long instancesLastCached;
    private transient long capacityCacheTime = 20;
    private transient int cloudCapacity;
    private transient long cloudCapacityLastCached;
    private transient Object capacityLock = new Object();

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

    public void setMaxConnections(int maxConnections) {
        this.communicator.setMaxConections(maxConnections);
    }

    public void setConnectionKeepAliveSeconds(int seconds) {
        this.communicator.setConnectionKeepAliveSeconds(seconds);
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

    public String startVM(String templateId, String tag, String nameTemplate, String startUpScript, String groupId, int priority, String name, String externalId, int vcpu, int vram) throws AnkaMgmtException {
        String id = communicator.startVm(templateId, tag, nameTemplate, startUpScript, groupId, priority, name, externalId, vcpu, vram);
        invalidateCache();
        return id;
    }

    public String startVM(String templateId, String tag, String startUpScript, String groupId, int priority, String name, String externalId, int vcpu, int vram) throws AnkaMgmtException {
        String id = communicator.startVm(templateId, tag, "$template_name-$node_name-$ts", startUpScript, groupId, priority, name, externalId, vcpu, vram);
        invalidateCache();
        return id;
    }

    public boolean terminateInstance(String vmId) throws AnkaMgmtException {
        LOGGER.info("Sending termination request to instance: "+ vmId);
        boolean result = communicator.terminateVm(vmId);
        invalidateCache();
        return result;
    }

    public AnkaVmInstance showInstance(String vmId) throws AnkaMgmtException {
        getNewData();
        return instances.get(vmId);
    }

    public List<AnkaVmInstance> listVms() throws AnkaMgmtException {
        return this.communicator.list();
    }

    public void cacheInstances(List<AnkaVmInstance> instances) {
            Map<String, AnkaVmInstance> cacheMap = new HashMap<>(instances.size());
            for (AnkaVmInstance instance: instances) {
                cacheMap.put(instance.id, instance);
            }
            instancesLastCached = System.currentTimeMillis();
            this.instances = cacheMap;
    }

    private void getNewData() throws AnkaMgmtException {
        synchronized (this) {
            if (instances == null || isCacheStale()) {
                List<AnkaVmInstance> ankaVmInstances = this.listVms();
                cacheInstances(ankaVmInstances);
            }
        }
    }

    private void invalidateCache() {
        synchronized (this) {
            this.instancesLastCached = 0;
        }
    }

    private boolean isCacheStale() {
        long currentTimeMillis = System.currentTimeMillis();
        long diffMillis = currentTimeMillis - instancesLastCached;
        long staleTimeout = TimeUnit.SECONDS.toMillis(instancesCacheTime);
        if (diffMillis > staleTimeout) {
            return true;
        }
        return false;
    }

    public int getCloudCapacity() throws AnkaMgmtException {
        synchronized (capacityLock) {
            long currentTimeMillis = System.currentTimeMillis();
            long diffMillis = currentTimeMillis - cloudCapacityLastCached;
            long staleTimeout = TimeUnit.SECONDS.toMillis(capacityCacheTime);
            if (cloudCapacity == 0 || diffMillis > staleTimeout) {
                List<AnkaNode> nodes = communicator.getNodes();
                int countCapacity = 0;
                for (AnkaNode node: nodes) {
                    if (node != null && node.isActive()) {
                        if (node.hasQuantityBasedCapacity()) {
                            countCapacity += node.getCapacity();
                        } else {
                            countCapacity += (node.getCapacity() / 2); // guess real capacity
                        }
                    }
                }
                cloudCapacity = countCapacity;
            }
        }
        return cloudCapacity;
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
