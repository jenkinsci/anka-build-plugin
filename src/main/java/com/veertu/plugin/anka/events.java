package com.veertu.plugin.anka;

import com.veertu.ankaMgmtSdk.AnkaMgmtVm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


enum Event {
    VMStarted,
    nodeStarted,
    saveImage,
    nodeTerminated
}

class AnkaEvent {
}

class VMStarted extends AnkaEvent {

    private AnkaMgmtVm vm;

    public VMStarted(AnkaMgmtVm vm) {

        this.vm = vm;
    }

    public AnkaMgmtVm getVm() {
        return vm;
    }
}

class NodeTerminated extends AnkaEvent {

    private AbstractAnkaSlave node;

    NodeTerminated(AbstractAnkaSlave node) {
        this.node = node;
    }

    public AbstractAnkaSlave getNode() {
        return node;
    }
}

class SaveImageEvent extends AnkaEvent {
    private AbstractAnkaSlave node;

    public SaveImageEvent(AbstractAnkaSlave node) {
        this.node = node;
    }

    public AbstractAnkaSlave getNode() {
        return node;
    }
}

class NodeStarted extends AnkaEvent {
    private AbstractAnkaSlave node;

    public NodeStarted(AbstractAnkaSlave node) {
        this.node = node;
    }

    public AbstractAnkaSlave getNode() {
        return node;
    }
}

class AnkaEvents {
    private static final Object mutex = new Object();
    private static Map<Event, List<EventHandler>> listeners = new HashMap<>();

    public static void addListener(Event event, EventHandler handler) {
        synchronized (mutex) {
            List<EventHandler> eventHandlers = listeners.get(event);
            if (eventHandlers == null) {
                eventHandlers = new ArrayList<>();
            }
            eventHandlers.add(handler);
            listeners.put(event, eventHandlers);
        }
    }

    public static void fire(Event event, final AnkaEvent e) {
        List<EventHandler> eventHandlers = listeners.get(event);
        for (final EventHandler hl : eventHandlers) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    hl.handle(e);
                }
            }).start();
        }
    }

}

interface EventHandler {
    void handle(AnkaEvent e);
}
