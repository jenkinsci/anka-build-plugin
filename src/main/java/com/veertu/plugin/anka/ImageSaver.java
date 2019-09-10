package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaAPI;
import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ImageSaver {

    public static void saveImage(AnkaMgmtCloud cloud, AbstractAnkaSlave slave, AnkaMgmtVm vm, SaveImageParameters saveImageParams) throws AnkaMgmtException {
        String tagToPush = saveImageParams.getTag();
        if (tagToPush == null || tagToPush.isEmpty()) {
            tagToPush = slave.getJobNameAndNumber();
        }
        tagToPush += "_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String latestTag = null;
        if (saveImageParams.isDeleteLatest()) {
            AnkaAPI api = cloud.getAnkaApi();
            List<String> tags = api.listTemplateTags(saveImageParams.getTemplateID());

            if (tags.size() > 1) {
                latestTag = tags.get(tags.size()-1);
            }
        }
        String shutdownScript = shutdownScript();
        vm.saveImage(saveImageParams.getTemplateID(), tagToPush, saveImageParams.getDescription(),
                saveImageParams.getSuspend(), shutdownScript, saveImageParams.isDeleteLatest(), latestTag, true );
    }

    private static String shutdownScript() {
        return "sync && sleep 60";
    }

}
