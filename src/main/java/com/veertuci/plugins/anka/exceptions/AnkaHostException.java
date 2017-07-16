package com.veertuci.plugins.anka.exceptions;

/**
 * Created by avia on 04/07/2016.
 */
public class AnkaHostException extends AnkaException {


    private static final long serialVersionUID = -6133908887091288919L;

    public AnkaHostException(String s) {
        super(s);
    }

    public AnkaHostException(Throwable cause){
        super(cause);
    }


    public AnkaHostException(String s, Throwable cause){
        super(s, cause);
    }

    public AnkaHostException(Exception ex) {
        super(ex);
    }
}
