package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaAPI;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;
import com.veertu.plugin.anka.exceptions.SaveImageStatusTimeout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ImageSaver {

    public static void markFuture(AnkaMgmtCloud cloud, AbstractAnkaSlave slave) {
        String buildId = slave.getJobNameAndNumber();
        SaveImageRequest saveImageRequest = new SaveImageRequest(cloud, buildId);
        SaveImageRequestsHolder requestsHolder = SaveImageRequestsHolder.getInstance();
        requestsHolder.setRequest(buildId, saveImageRequest);
    }

    public static void saveImage(AnkaMgmtCloud cloud, AbstractAnkaSlave slave) throws AnkaMgmtException {
        try {

            String buildId = slave.getJobNameAndNumber();
            SaveImageRequestsHolder requestsHolder = SaveImageRequestsHolder.getInstance();
            List<SaveImageRequest> saveImageRequests = requestsHolder.getRequests(buildId);
            SaveImageRequest saveImageRequest = null;
            for (SaveImageRequest req : saveImageRequests) {
                if (req.isFuture() && req.getRequestId() == null) {
                    saveImageRequest = req;
                }
            }
            if (saveImageRequest == null) {  // no request in "future" state found
                saveImageRequest = new SaveImageRequest(cloud, buildId);
            }

            saveImageRequest.requesting();

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
                    latestTag = tags.get(tags.size() - 1);
                    deleteLatest = true;
                }
            }
            String shutdownScript = shutdownScript();


            String reqId = cloud.getAnkaApi().saveImage(slave.getInstanceId(), template.getTemplateId(), tagToPush, template.getDescription(),
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
            while (true) {
                List<SaveImageRequest> requests = requestsHolder.getRequests(buildId);
                if (requests.size() <= 0) {
                    return true;
                }
                for (int i = requests.size() - 1; i >= 0; i--) {
                    SaveImageRequest request = requests.get(i);
                    if (request == null) {
                        continue;
                    }
                    SaveImageState state = request.checkState();
                    if (state == SaveImageState.Future) {
                        AnkaMgmtCloud.Log("Save Image request haven't started yet");
                        Thread.sleep(1000 * retryWaitTimeSeconds);
                        break;
                    }
                    if (state == SaveImageState.Pending || state == SaveImageState.Requesting) {
                        if ((System.currentTimeMillis() - startTime) > (timeoutMinutes * 60 * 1000)) {
                            throw new SaveImageStatusTimeout();
                        }
                        Thread.sleep(1000 * retryWaitTimeSeconds);
                        break;
                    }
                    if (state == SaveImageState.Error || state == SaveImageState.Timeout) {
                        AnkaMgmtCloud.Log("Save image failed. Anka Cloud Name: %s, Request id: %s",
                                request.getCloud().getDisplayName(), request.getRequestId());
                        return false;
                    }
                    if (state == SaveImageState.Done) {
                        return true;
                    }
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return true;
    }

    private static String shutdownScript() {
        return "sync && sleep 60";
    }

    public static void deleteRequest(AbstractAnkaSlave node) {
        if (node != null) {
            String buildId = node.getJobNameAndNumber();
            SaveImageRequestsHolder requestsHolder = SaveImageRequestsHolder.getInstance();
            requestsHolder.runFinished(buildId);
        }
    }
}
