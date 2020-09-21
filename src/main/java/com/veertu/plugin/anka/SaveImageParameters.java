package com.veertu.plugin.anka;

import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class SaveImageParameters {

    public static String saveImageKey = "saveImage";
    public static String templateIDKey = "templateId";
    public static String tagKey = "tag";
    public static String appendTimestampKey = "appendTimestamp";
    public static String deleteLatestKey = "deleteLatest";
    public static String descriptionKey = "description";
    public static String suspendKey = "suspend";
    public static String waitForBuildToFinishKey = "wait_for_build_to_finish";

    protected Boolean waitForBuildToFinish;
    protected Boolean suspend;
    protected String description;

    protected Boolean saveImage;

    protected String templateID;
    protected String tag;
    protected Boolean appendTimestamp;
    protected Boolean deleteLatest;


    public Boolean getWaitForBuildToFinish() {
        return waitForBuildToFinish;
    }

    public Boolean getSuspend() {
        return suspend;
    }

    public Boolean getSaveImage() {
        if (saveImage == null)
            return false;
        return saveImage;
    }

    public String getTemplateID() {
        return templateID;
    }

    public String getTag() {
        return tag;
    }

    public boolean isAppendTimestamp() {
        return appendTimestamp;
    }

    public boolean isDeleteLatest() {
        return deleteLatest;
    }

    public String getDescription() {
        return description;
    }


    public void setWaitForBuildToFinish(Boolean waitForBuildToFinish) {
        this.waitForBuildToFinish = waitForBuildToFinish;
    }

    @DataBoundSetter
    public void setSuspend(Boolean suspend) {
        this.suspend = suspend;
    }

    @DataBoundSetter
    public void setDescription(String description) {
        this.description = description;
    }

    @DataBoundSetter
    public void setSaveImage(Boolean saveImage) {
        this.saveImage = saveImage;
    }

    @DataBoundSetter
    public void setTemplateID(String templateID) {
        this.templateID = templateID;
    }

    @DataBoundSetter
    public void setTag(String tag) {
        this.tag = tag;
    }

    @DataBoundSetter
    public void setAppendTimestamp(boolean appendTimestamp) {
        this.appendTimestamp = appendTimestamp;
    }

    @DataBoundSetter
    public void setDeleteLatest(Boolean deleteLatest) {
        this.deleteLatest = deleteLatest;
    }

    public SaveImageParameters() {

    }

    @DataBoundConstructor
    public SaveImageParameters(Boolean saveImage, String templateID, String tag,
                               Boolean appendTimestamp, Boolean deleteLatest, String description,
                               Boolean suspend, Boolean waitForBuildToFinish) {
        this.saveImage = saveImage;
        this.templateID = templateID;
        this.tag = tag;
        this.appendTimestamp = appendTimestamp;
        this.deleteLatest = deleteLatest;
        this.waitForBuildToFinish = waitForBuildToFinish;
        this.description = description;
        this.suspend = suspend;
    }

    public static SaveImageParameters fromJson(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        return new SaveImageParameters(
                jsonObject.optBoolean(saveImageKey, false),
                jsonObject.optString(templateIDKey, null),
                jsonObject.optString(tagKey, null),
                jsonObject.optBoolean(appendTimestampKey, true),
                jsonObject.optBoolean(deleteLatestKey, false),
                jsonObject.optString(descriptionKey, null),
                jsonObject.optBoolean(suspendKey, false),
                jsonObject.optBoolean(waitForBuildToFinishKey, false)
                );
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(saveImageKey, saveImage);
        jsonObject.put(templateIDKey, templateID);
        jsonObject.put(tagKey, tag);
        jsonObject.put(deleteLatestKey, deleteLatest);
        jsonObject.put(descriptionKey, description);
        jsonObject.put(suspendKey, suspend);
        jsonObject.put(waitForBuildToFinishKey, waitForBuildToFinish);
        return jsonObject;
    }

}
