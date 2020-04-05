package com.veertu.plugin.anka;

import hudson.model.Node;
import jenkins.model.Jenkins;

import java.io.File;
import java.util.*;

public class SaveImageRequestsHolder extends AnkaDataSaver {

    private static final transient SaveImageRequestsHolder instance = new SaveImageRequestsHolder();
    //          job id , list of requests
    private Map<String, List<SaveImageRequest>> requests;
    private final transient Object mutex;

    private SaveImageRequestsHolder() {
        super();
        requests = new HashMap<>();
        mutex = new Object();
        load();
    }

    @Override
    protected String getClassName() {
        return "Save Image Requests Holder";
    }

    @Override
    protected File getConfigFile() {
        return new File(Jenkins.getInstance().getRootDir(), "jenkins.plugins.anka.saveImageRequestsHolder.xml");
    }

    public static SaveImageRequestsHolder getInstance() {
        return instance;
    }

    public void setRequest(String jobId, SaveImageRequest request) {
        List<SaveImageRequest> requestList = getListOfRequests(jobId);
        synchronized (mutex) {
            requestList.add(request);
        }
        save();

    }

    public List<SaveImageRequest> getRequests(String jobId) {
        return getListOfRequests(jobId);
    }


    public void runFinished(String buildId) {
        requests.remove(buildId);
        save();
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

    public void clean() {
        // Requests are saved using job "id".
        // So we first create a list of all existing job IDs, and then remove what does not exist

        Map<String, Boolean> ankaSlaveMap = new HashMap<>();
        List<Node> nodes = Jenkins.getInstance().getNodes();
        Iterator<Node> iterator = nodes.iterator();
        while(iterator.hasNext()) {
            Node node = iterator.next();

            if (node instanceof AbstractAnkaSlave)
                ankaSlaveMap.put(((AbstractAnkaSlave) node).getJobNameAndNumber(), true);
        }

        Iterator<String> jobIterator = requests.keySet().iterator();
        while (jobIterator.hasNext()) {
            String jobId = jobIterator.next();

            Boolean doesJobExist = ankaSlaveMap.get(jobId);
            if (doesJobExist == null)
                jobIterator.remove();
        }
    }
}
