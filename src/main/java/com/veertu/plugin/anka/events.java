package com.veertu.plugin.anka;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class AnkaEvent {
}

class VMStarted extends AnkaEvent {

    private String vmID;

    public VMStarted(String vmID) {

        this.vmID = vmID;
    }

    public String getVMId() {
        return vmID;
    }
}

class NodeTerminated extends AnkaEvent {

    private String nodeName;

    NodeTerminated(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getNodeName() {
        return nodeName;
    }
}

class AnkaEvents {
    private static final Object mutex = new Object();
    private static Map<String, List<EventHandler>> listeners = new HashMap<>();

    public static void addListener(String event, EventHandler handler) {
        synchronized (mutex) {
            List<EventHandler> eventHandlers = listeners.get(event);
            if (eventHandlers == null) {
                eventHandlers = new ArrayList<>();
            }
            eventHandlers.add(handler);
            listeners.put(event, eventHandlers);
        }
    }

    public static void fire(String event, final AnkaEvent e) {
        List<EventHandler> eventHandlers = listeners.get(event);
        for (final EventHandler hl : eventHandlers) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    hl.handle(e);
                }
            }).run();
        }
    }

}

interface EventHandler {
    void handle(AnkaEvent e);
}
