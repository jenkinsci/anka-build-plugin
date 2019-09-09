package com.veertu.plugin.anka;

import net.sf.json.JSONObject;

public class SaveImageParameters {

    public static String saveImageKey = "saveImage";
    public static String templateIDKey = "templateId";
    public static String tagKey = "tag";
    public static String deleteLatestKey = "deleteLatest";
    public static String descriptionKey = "description";
    public static String suspendKey = "suspend";

    private final Boolean saveImage;

    private final String templateID;
    private final String tag;
    private final Boolean deleteLatest;

    public Boolean getSuspend() {
        return suspend;
    }

    private final Boolean suspend;

    public Boolean getSaveImage() {
        return saveImage;
    }

    public String getTemplateID() {
        return templateID;
    }

    public String getTag() {
        return tag;
    }

    public boolean isDeleteLatest() {
        return deleteLatest;
    }

    public String getDescription() {
        return description;
    }

    private final String description;

    public SaveImageParameters(Boolean doSaveImage, String templateID, String tag, Boolean deleteLatest, String description, Boolean suspend) {
        this.saveImage = doSaveImage;
        this.templateID = templateID;
        this.tag = tag;
        this.deleteLatest = deleteLatest;
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
                jsonObject.optBoolean(deleteLatestKey, false),
                jsonObject.optString(descriptionKey, null),
                jsonObject.optBoolean(suspendKey, false)
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
        return jsonObject;
    }
}
