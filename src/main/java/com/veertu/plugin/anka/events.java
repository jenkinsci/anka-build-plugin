package com.veertu.plugin.anka;

import java.util.ArrayList;
import java.util.List;

interface VmStartedListener {
    void vmStarted(String nodeName, String vmId);
}

class VmStartedEvent {
    private static List<VmStartedListener> listeners = new ArrayList<>();

    public static void addListener(VmStartedListener toAdd) {
        listeners.add(toAdd);
    }

    public static void vmStarted(final String nodeName, final String vmId) {
        for (final VmStartedListener hl : listeners) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    hl.vmStarted(nodeName, vmId);
                }
            }).run();
        }
    }
}

interface NodeTerminatedListener {
    void nodeTerminated(String nodeName);
}

class NodeTerminatedEvent {
    private static List<NodeTerminatedListener> listeners = new ArrayList<>();

    public static void addListener(NodeTerminatedListener toAdd) {
        listeners.add(toAdd);
    }

    public static void nodeTerminated(final String nodeName) {
        for (final NodeTerminatedListener hl : listeners) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    hl.nodeTerminated(nodeName);
                }
            }).run();
        }
    }
}
