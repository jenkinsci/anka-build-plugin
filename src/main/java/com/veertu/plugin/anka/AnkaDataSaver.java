package com.veertu.plugin.anka;

import hudson.XmlFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;

import static com.veertu.plugin.anka.AnkaMgmtCloud.Log;

abstract class AnkaDataSaver {
    // This class can be implemented to utilize jenkins persistence

    private transient final Object persistenceLock;

    public AnkaDataSaver() {
        persistenceLock = new Object();
    }

    protected abstract String getClassName();  // Used for logging
    protected abstract File getConfigFile();  // Used for file path and name

    protected void load() {
        synchronized (persistenceLock) {
            try {
                File f = getConfigFile();
                boolean isNew = f.createNewFile();
                if (!isNew)
                    new XmlFile(f).unmarshal(this);
            } catch (NoSuchFileException e) {
                Log(getClassName() + ": Persistence file does not exist");
            } catch (IOException e) {
                Log(getClassName() + ": Failed to load data from file. Error: %s", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    protected void save() {
        synchronized (persistenceLock) {
            try {
                new XmlFile(getConfigFile()).write(this);
            } catch (IOException e) {
                Log(getClassName() + ": Failed to save changes to file. Error: %s", e.getMessage());
                e.printStackTrace();
            }
        }
    }

}
