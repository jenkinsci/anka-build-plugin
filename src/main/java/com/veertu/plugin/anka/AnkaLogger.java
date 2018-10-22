package com.veertu.plugin.anka;

/**
 * Created by avia on 09/07/2016.
 */
public interface AnkaLogger {

    void logInfo(String message);
    void logWarning(String message);
    void logError(String message);
    void logFatalError(String message);
}
