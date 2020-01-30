package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaAPI;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;

public class KillConfirmer implements VmStartedListener, NodeTerminatedListener{

    public static int waitTime = 10 * 1000; // 10 seconds
    private VmIdToNode vmIdToNode;
    private AnkaAPI ankaAPI;

    public KillConfirmer(AnkaAPI ankaAPI) {
        this.vmIdToNode = new VmIdToNode();
        this.ankaAPI = ankaAPI;
    }


    @Override
    public void vmStarted(String nodeName, String vmId) {
        vmIdToNode.add(vmId, nodeName);
    }

    @Override
    public void nodeTerminated(String nodeName) {
        String vmId = vmIdToNode.findVmId(nodeName);
        if (vmId == null) {
            return;
        }
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            AnkaMgmtCloud.Log("node terminated sleep interrupted");
        }

        try {
            ankaAPI.terminateInstance(vmId);
        } catch (AnkaMgmtException e) {
            AnkaMgmtCloud.Log("could not terminate " + vmId);
        } finally {
            vmIdToNode.remove(nodeName);
        }
    }
}
