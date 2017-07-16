package com.veertuci.plugins.anka;

/**
 * Created by avia on 09/07/2016.
 */
public interface AnkaLogger {

    public void logInfo(String message);
    public void logWarning(String message);
    public void logError(String message);
    public void logFatalError(String message);
}
