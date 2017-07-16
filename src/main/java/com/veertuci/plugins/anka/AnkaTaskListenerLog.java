package com.veertuci.plugins.anka;

import hudson.model.TaskListener;

/**
 * Created by avia on 09/07/2016.
 */
public class AnkaTaskListenerLog implements AnkaLogger {

    private final TaskListener taskListener;
    private final String logPrefix;

    public AnkaTaskListenerLog(TaskListener taskLister, String logPrefix) {
        this.taskListener = taskLister;
        this.logPrefix = logPrefix;
    }

  /* log methods from AnkaLogger */

    public void logInfo(String message) {
        taskListener.getLogger().println(logPrefix + message);
    }

    public void logWarning(String message) {
        taskListener.error(logPrefix + message);
    }

    public void logError(String message) {
        taskListener.error(logPrefix + message);
    }

    public void logFatalError(String message) {
        taskListener.fatalError(logPrefix + message);
    }


}
