package com.veertu.plugin.anka.exceptions;

/**
 * Created by asafgur on 07/12/2016.
 */
public class AnkaHostConfigurationException extends AnkaHostException {

    public AnkaHostConfigurationException(String s) {
        super(s);
    }

    public AnkaHostConfigurationException(Throwable e){
        super(e);
    }

    public AnkaHostConfigurationException(String s, Throwable cause) {
        super(s, cause);
    }
}
