package com.veertu.plugin.anka;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by avia on 09/07/2016.
 */
public class AnkaSystemLog implements AnkaLogger {

    private final transient Logger logger;
    private final String logPrefix;

    public AnkaSystemLog(Logger logger, String logPrefix) {
        this.logger = logger;
        this.logPrefix = logPrefix;
    }

    public void logInfo(String message) {
        logger.log(Level.INFO, "{0}{1}", new Object[]{logPrefix, message});
    }

    public void logWarning(String message) {
        logger.log(Level.WARNING, "{0}{1}", new Object[]{logPrefix, message});
    }

    public void logError(String message) {
        logger.log(Level.SEVERE, "{0}{1}", new Object[]{logPrefix, message});
    }

    public void logFatalError(String message) {
        logger.log(Level.SEVERE, "{0}{1}", new Object[]{logPrefix, message});
    }


}
