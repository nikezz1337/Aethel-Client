package dev.aethel.event.list;

import dev.aethel.event.Event;

public class EventKeyInput extends Event {
    private final int key;
    private final int action;

    public EventKeyInput(int key, int action) {
        this.key = key;
        this.action = action;
    }

    public int getKey() {
        return key;
    }

    public int getAction() {
        return action;
    }
}
