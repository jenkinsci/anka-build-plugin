package com.veertu.plugin.anka;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SaveImageRequestsHolder {

    private static SaveImageRequestsHolder instance = new SaveImageRequestsHolder();
    //          job id , list of requests
    private Map<String, List<SaveImageRequest>> requests;
    private final Object mutex;

    public SaveImageRequestsHolder() {
        requests = new HashMap<>();
        mutex = new Object();
    }

    public static SaveImageRequestsHolder getInstance() {
        return instance;
    }

    public void setRequest(String jobId, SaveImageRequest request) {
        List<SaveImageRequest> requestList = getListOfRequests(jobId);
        synchronized (mutex) {
//            SaveImageRequest request = new SaveImageRequest(jobId);
            requestList.add(request);
        }

    }

    public List<SaveImageRequest> getRequests(String jobId) {
        return getListOfRequests(jobId);
    }


    public void runFinished(String buildId) {
        requests.remove(buildId);
    }

    private List<SaveImageRequest> getListOfRequests(String jobId) {
        synchronized (mutex) {
            List<SaveImageRequest> listOfRequests = requests.get(jobId);
            if (listOfRequests == null) {
                listOfRequests = new ArrayList<>();
                requests.put(jobId, listOfRequests);
            }
            return listOfRequests;
        }
    }

}
