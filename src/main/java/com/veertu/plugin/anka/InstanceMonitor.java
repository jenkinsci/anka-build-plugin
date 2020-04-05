package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.ankaMgmtSdk.exceptions.VmAlreadyTerminatedException;
import jenkins.model.Jenkins;

import java.io.File;
import java.util.*;

import static com.veertu.plugin.anka.AnkaMgmtCloud.Log;

class Instance {
    public String cloudName;
    public String vmId;
    public String nodeName;
    public boolean isSaveImage;
    public long creationTimestamp;
    public long nodeDisappearedTimestamp;

    @Override
    public String toString() {
        return "Instance{" +
                "cloudName='" + cloudName + '\'' +
                ", vmId='" + vmId + '\'' +
                ", nodeName='" + nodeName + '\'' +
                ", isSaveImage=" + isSaveImage +
                ", creationTimestamp=" + creationTimestamp +
                ", nodeDisappearedTimestamp=" + nodeDisappearedTimestamp +
                '}';
    }
}

public class InstanceMonitor extends AnkaDataSaver {

    private static transient final InstanceMonitor monitor = new InstanceMonitor();
    private static transient boolean shouldDebug = false;

    private final transient Object mutex;
    private final transient Object runningLock;
    private transient boolean isRunning;
    private transient Map<String, Instance> vmMap;  // vm id -> instance object

    private List<Instance> instances;

    public static InstanceMonitor getInstance() {
        return monitor;
    }

    private InstanceMonitor() {
        super();
        mutex = new Object();
        runningLock = new Object();
        vmMap = new HashMap<>();
        isRunning = false;
        instances = new ArrayList<>();
        load();
    }

    // Debug

    public static void setDebug(boolean state) {
        shouldDebug = state;
        Log("Instance Monitor: Setting debug to" + state);
    }

    private void debugLog(String msg) {
        if (shouldDebug) {
            Log("Instance Monitor Debug: %s", msg);
        }
    }

    // Persistence

    protected void load() {
        debugLog("Loading from file");
        super.load();
        debugLog("Loading done successfully");

        synchronized (mutex) {
            Iterator<Instance> it = instances.iterator();
            while (it.hasNext()) {
                Instance instance = it.next();
                vmMap.put(instance.vmId, instance);
            }
        }

        runMonitoringThread();
    }

    protected void save() {
        debugLog("Saving to file");
        super.save();
        debugLog("Saved successfully");
    }

    @Override
    protected File getConfigFile() {
        return new File(Jenkins.getInstance().getRootDir(), "jenkins.plugins.anka.instanceMonitor.xml");
    }

    @Override
    protected String getClassName() {
        return "Instance Monitor";
    }

    public void migrateFromOldDaemon(String cloudName, InstanceDaemon daemon) {
        // Since this occurs only once per plugin and in the future wont happen at all
        // this naive implementation is OK for now

        synchronized (mutex) {

            List<Instance> previousInstances = new ArrayList<>();

            Map<String, String> nodeNamesToVm = daemon.getNodeNameToVmId();
            Map<String, String> vmsToNodeNames = daemon.getVmIdToNodeName();

            Iterator<String> it = vmsToNodeNames.keySet().iterator();
            while (it.hasNext()) {
                String vmId = it.next();
                Instance instance = createBasicInstance(cloudName, vmId);
                if (daemon.isPushing(vmId))
                    instance.isSaveImage = true;
                String nodeName = vmsToNodeNames.get(vmId);
                if (nodeName != null && !nodeName.equals("")) {
                    instance.nodeName = nodeName;
                    nodeNamesToVm.remove(nodeName);
                }
                previousInstances.add(instance);
            }

            it = nodeNamesToVm.keySet().iterator();
            while (it.hasNext()) {
                String unknownNodeName = it.next();
                String vmId = nodeNamesToVm.get(unknownNodeName);
                if (vmId != null && !vmId.equals("")) {
                    Instance instance = createBasicInstance(cloudName, vmId);
                    if (daemon.isPushing(vmId)) {
                        instance.isSaveImage = true;
                        instance.nodeName = unknownNodeName;
                    }
                    previousInstances.add(instance);
                }
            }

            instances.addAll(previousInstances);
            save();
        }
    }

    // Events

    public void vmStarted(String cloudName, String vmId) {
        debugLog(String.format("vm started event. cloud: %s, vm id: %s", cloudName, vmId));
        synchronized (mutex) {
            Instance instance = createBasicInstance(cloudName, vmId);
            instances.add(instance);
            vmMap.put(vmId, instance);
            save();
        }
    }

    public void nodeStarted(String cloudName, String nodeName, String vmId) {
        debugLog(String.format("node started event. cloud: %s, vm id: %s, node name: %s", cloudName, vmId, nodeName));
        synchronized (mutex) {
            Instance instance = vmMap.get(vmId);
            if (instance == null) {
                Log("Instance Monitor (node started): Instance monitor has no VM %s registered. Adding it manually", vmId);
                instance = createBasicInstance(cloudName, vmId);
                instances.add(instance);
                vmMap.put(vmId, instance);
            }
            instance.nodeName = nodeName;
            save();
        }
    }

    public void saveImageSent(String cloudName, String nodeName, String vmId) {
        debugLog(String.format("save image event. cloud: %s, vm id: %s, node name: %s", cloudName, vmId, nodeName));
        synchronized (mutex) {
            Instance instance = vmMap.get(vmId);
            if (instance == null) {
                Log("Instance Monitor (save image sent): Instance monitor has no VM %s registered", vmId);
                instance = createBasicInstance(cloudName, vmId);
                instances.add(instance);
                vmMap.put(vmId, instance);
            }
            instance.isSaveImage = true;
            save();
        }
    }

    // Logic

    private Instance createBasicInstance(String cloudName, String vmId) {
        debugLog(String.format("Creating new instance. Cloud name: %s, vm id: %s", cloudName, vmId));
        Instance instance = new Instance();
        instance.cloudName = cloudName;
        instance.vmId = vmId;
        instance.isSaveImage = false;
        instance.creationTimestamp = System.currentTimeMillis();
        instance.nodeDisappearedTimestamp = 0L;
        return instance;
    }

    private void runMonitoringThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Prevent from running more than once
                synchronized (runningLock) {
                    if (isRunning) {
                        Log("Instance monitor: preventing runner from running twice");
                        return;
                    }
                    isRunning = true;
                }

                while (true) {
                    synchronized (mutex) {
                        boolean saveRequired = false;
                        Iterator<Instance> it = instances.iterator();
                        while (it.hasNext()) {
                            Instance instance = it.next();

                            try {

                                String vmState = getVmState(instance);
                                if (vmState.equals("")) {
                                    // VM Does not exist -> stop monitoring
                                    debugLog(String.format("vm does not exist in controller. instance: %s", instance));
                                    it.remove();
                                    vmMap.remove(instance.vmId);
                                    saveRequired = true;
                                }

                                if (vmState.equals("Scheduling") || vmState.equals("Pulling"))
                                    continue;

                                // VM is alive, but no node yet
                                if (instance.nodeName == null || instance.nodeName.equals("")) {
                                    debugLog(String.format("vm is alive and node was not assigned yet. instance: %s", instance));
                                    killVmOnTimeout(instance, instance.creationTimestamp);
                                }


                                // VM is alive and had a node at some point, but node is now gone
                                // This can happen if save image is running and node is already gone
                                // or if jenkins had a restart and did not bring the node back up yet
                                if (!doesNodeExists(instance)) {
                                    debugLog(String.format("vm is alive and a node was assigned. instance: %s", instance));

                                    // Note down when node first disappeared
                                    if (instance.nodeDisappearedTimestamp == 0L) {
                                        instance.nodeDisappearedTimestamp = System.currentTimeMillis();
                                    }

                                    if (!instance.isSaveImage)
                                        killVmOnTimeout(instance, instance.nodeDisappearedTimestamp);
                                } else {
                                    // Reset timestamp in case node returns (restarts, for example)
                                    instance.nodeDisappearedTimestamp = 0L;
                                }

                                // VM and node are alive, do nothing
                            } catch (AnkaMgmtException e) {
                                Log("Instance Monitor: Ignoring VM %s due to error: %s", instance.vmId, e.getMessage());
                                e.printStackTrace();
                            }
                        }
                        if (saveRequired)
                            save();
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Log("Instance Monitor: Got exception while waiting between loop executions");
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private String getVmState(Instance instance) throws AnkaMgmtException {
        AnkaMgmtCloud cloud = (AnkaMgmtCloud) Jenkins.getInstance().getCloud(instance.cloudName);
        if (cloud == null)
            return "";
        return cloud.getInstanceStatus(instance.vmId);
    }

    private boolean doesNodeExists(Instance instance) {
        return (Jenkins.getInstance().getNode(instance.nodeName) == null);
    }

    private void killVmOnTimeout(Instance instance, long timestamp) throws AnkaMgmtException {
        final int NO_NODE_TIMEOUT = 1000 * 60 * 10; // 10 minutes

        if (System.currentTimeMillis() - timestamp > NO_NODE_TIMEOUT) {
            Log("Instance Monitor: Terminating VM %s (gave up on getting a node)...", instance.vmId);
            AnkaMgmtCloud cloud = (AnkaMgmtCloud) Jenkins.getInstance().getCloud(instance.cloudName);
            if (cloud == null) {
                Log("Instance Monitor: Termination failed. Could not retrieve cloud %s", instance.cloudName);
                return;
            }
            try {
                cloud.terminateVm(instance.vmId);
            } catch (VmAlreadyTerminatedException e) {
                Log("Instance Monitor: VM %s Already terminated", instance.vmId);
            }
        }
    }

}