package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaAPI;
import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.plugin.anka.exceptions.SaveImageStatusTimeout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ImageSaver {

    private Map<String, ArrayList<String>> requestIds;

    public ImageSaver() {
        this.requestIds = new HashMap<String, ArrayList<String>>();
    }

    public void saveReqId(String buildId, String reqId) {

        if (!this.requestIds.containsKey(buildId) ) {
            ArrayList<String> emptyList = new ArrayList<String>();
            this.requestIds.put(buildId, emptyList);
        }

        ArrayList<String> l = this.requestIds.get(buildId);
        l.add(reqId);
        this.requestIds.put(buildId, l);
    }

    public ArrayList<String> getReqIdsByBuild(String buildId) {
        ArrayList<String> v = this.requestIds.get(buildId);
        if ( v == null)
            return new ArrayList<String>();
        return v;
    }

    public void removeSaveImageReqs(String buildId) {
        this.requestIds.remove(buildId);
    }

    public static void saveImage(AnkaMgmtCloud cloud, AbstractAnkaSlave slave, AnkaMgmtVm vm, SaveImageParameters saveImageParams) throws AnkaMgmtException {
        String tagToPush = saveImageParams.getTag();
        if (tagToPush == null || tagToPush.isEmpty()) {
            tagToPush = slave.getJobNameAndNumber();
        }
        tagToPush += "_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        boolean deleteLatest = false;
        String latestTag = null;
        if (saveImageParams.isDeleteLatest()) {
            AnkaAPI api = cloud.getAnkaApi();
            List<String> tags = api.listTemplateTags(saveImageParams.getTemplateID());

            if (tags.size() > 1) {
                latestTag = tags.get(tags.size()-1);
                deleteLatest = true;
            }
        }
        String shutdownScript = shutdownScript();
        String reqId = vm.saveImage(saveImageParams.getTemplateID(), tagToPush, saveImageParams.getDescription(),
                saveImageParams.getSuspend(), shutdownScript, deleteLatest, latestTag, true );
        if (reqId == "") {
            throw new AnkaMgmtException("missing save image request ID");
        }
        cloud.addSaveImageReq(slave.getJobNameAndNumber(), reqId);
    }

    private static String getStatus(AnkaMgmtCloud cloud, String reqId) throws AnkaMgmtException {
        AnkaAPI api = cloud.getAnkaApi();
        return api.getSaveImageStatus(reqId);
    }

    public static boolean isSuccessful(AnkaMgmtCloud cloud, String buildId, int timeoutMinutes) throws AnkaMgmtException, SaveImageStatusTimeout {
        int retryWaitTimeSeconds = 20;
        long startTime = System.currentTimeMillis();
        boolean keepReqId = false;

        try {
            List<String> reqIds = cloud.getSaveImageReqIds(buildId);
            for (String reqId : reqIds) {
                while (true) {
                    String status = getStatus(cloud, reqId);
                    if (status.equals("pending")) {
                        if ( (System.currentTimeMillis() - startTime) > (timeoutMinutes * 60 * 1000) ) {
                            keepReqId = true;
                            throw new SaveImageStatusTimeout();
                        }
                        Thread.sleep(1000 * retryWaitTimeSeconds);
                    } else {
                        if (status.equals("done")) {
                            break;
                        }
                        else {
                            AnkaMgmtCloud.Log("Save image failed. Anka Cloud Name: %s, Request id: %s", cloud.getDisplayName(), reqId);
                            return false;
                        }
                    }
                }
            }
        } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
        }
        finally {
            if (!keepReqId)
                cloud.removeSaveImageReqs(buildId);
        }

        return true;
    }

    private static String shutdownScript() {
        return "sync && sleep 60";
    }

}
