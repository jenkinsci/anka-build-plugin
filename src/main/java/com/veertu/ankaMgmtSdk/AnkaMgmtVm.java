package com.veertu.ankaMgmtSdk;

import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;

import java.io.IOException;

/**
 * Created by asafgur on 18/05/2017.
 */
public interface AnkaMgmtVm {

    String waitForBoot(int schedulingTimeout) throws InterruptedException, IOException, AnkaMgmtException;
    String getId();
    String getName();
    String getConnectionIp();
    int getConnectionPort();
    void terminate();
    boolean isRunning();
    String getInfo();

    String saveImage(String targetVMId, String tag,
                   String description, Boolean suspend,
                   String shutdownScript,
                   Boolean revertBeforePush,
                   String revertTag,
                   Boolean doSuspendTest) throws AnkaMgmtException;
}
