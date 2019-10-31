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

    public static void saveImage(AnkaMgmtCloud cloud, AbstractAnkaSlave slave, AnkaMgmtVm vm) throws AnkaMgmtException {
        try {

            AnkaCloudSlaveTemplate template = slave.getTemplate();
            String tagToPush = template.getPushTag();
            if (tagToPush == null || tagToPush.isEmpty()) {
                tagToPush = slave.getJobNameAndNumber();
            }
            tagToPush += "_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            boolean deleteLatest = false;
            String latestTag = null;
            if (template.isDeleteLatest()) {
                AnkaAPI api = cloud.getAnkaApi();
                List<String> tags = api.listTemplateTags(template.getTemplateId());

                if (tags.size() > 1) {
                    latestTag = tags.get(tags.size()-1);
                    deleteLatest = true;
                }
            }
            String shutdownScript = shutdownScript();
            String buildId = slave.getJobNameAndNumber();
            SaveImageRequest saveImageRequest = new SaveImageRequest(cloud, buildId);
            cloud.setSaveImageRequest(buildId, saveImageRequest);
            String reqId = vm.saveImage(template.getTemplateId(), tagToPush, template.getDescription(),
                    template.getSuspend(), shutdownScript, deleteLatest, latestTag, true);
            if (reqId.equals("")) {
                throw new AnkaMgmtException("missing save image request ID");
            }
            saveImageRequest.setRequestId(reqId);
        } catch (Exception e) {
            throw e;
        }
    }

    public static boolean isSuccessful(String buildId, int timeoutMinutes) throws AnkaMgmtException, SaveImageStatusTimeout {
        int retryWaitTimeSeconds = 20;
        long startTime = System.currentTimeMillis();
        SaveImageRequestsHolder requestsHolder = SaveImageRequestsHolder.getInstance();

        try {
            List<SaveImageRequest> requests = requestsHolder.getRequests(buildId);
            for (SaveImageRequest request: requests) {
                if (request == null) {
                    continue;
                }
                while (true) {
                    SaveImageState state = request.checkState();
                    if (state == SaveImageState.Pending || state == SaveImageState.Requesting) {
                        if ( (System.currentTimeMillis() - startTime) > (timeoutMinutes * 60 * 1000) ) {
                            throw new SaveImageStatusTimeout();
                        }
                        Thread.sleep(1000 * retryWaitTimeSeconds);
                    }
                    if (state == SaveImageState.Error || state == SaveImageState.Timeout) {
                        AnkaMgmtCloud.Log("Save image failed. Anka Cloud Name: %s, Request id: %s",
                                request.getCloud().getDisplayName(), request.getRequestId());
                        return false;
                    }
                    if (state == SaveImageState.Done) {
                        break;
                    }

                }
            }
        } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
        }
        return true;
    }

    private static String shutdownScript() {
        return "sync && sleep 60";
    }

}
