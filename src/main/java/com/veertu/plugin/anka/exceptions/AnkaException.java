package com.veertu.plugin.anka.exceptions;

/**
 * Created by asafgur on 28/11/2016.
 */
public class AnkaException extends Exception {

    public AnkaException() {
        super();
    }

    public AnkaException(String message) {
        super("Anka Error: " + message);
    }

    public AnkaException(String message, Throwable cause) {
        super("Anka Error: " + message, cause);
    }

    public AnkaException(Throwable cause) {
        super(cause);
    }
}
