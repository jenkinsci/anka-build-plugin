package com.veertu.plugin.anka;

import jenkins.model.Jenkins;

import java.io.File;

public class PersistenceManager extends AnkaDataSaver {
    private static transient final PersistenceManager instance = new PersistenceManager();
    private final transient Object versionLock;
    private int currentVersion;

    public static PersistenceManager getInstance() {
        return instance;
    }

    private PersistenceManager() {
        super();
        versionLock = new Object();
        currentVersion = 0;
        load();
    }

    @Override
    protected String getClassName() {
        return "Persistence Manager";
    }

    @Override
    protected File getConfigFile() {
        return new File(Jenkins.getInstance().getRootDir(), "jenkins.plugins.anka.persistenceManager.xml");
    }

    public void setToVersion(int persistenceVersion) {
        synchronized (versionLock) {
            currentVersion = persistenceVersion;
        }
        save();
    }

    public int getCurrentVersion() {
        synchronized (versionLock) {
            return currentVersion;
        }
    }

    public boolean isUpdateRequired(int persistenceVersion) {
        synchronized (versionLock) {
            return (currentVersion < persistenceVersion);
        }
    }
}
