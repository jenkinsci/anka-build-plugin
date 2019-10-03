package com.veertu.plugin.anka.exceptions;

/**
 * Created by asafgur on 15/01/2017.
 */
public class SaveImageStatusTimeout extends AnkaException {

    public SaveImageStatusTimeout() {
        super("Get status of save image request has timed out");
    }
}
