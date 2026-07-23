package dev.aethel.event.list;

import lombok.AllArgsConstructor;
import lombok.Getter;
import dev.aethel.event.Event;

@Getter
@AllArgsConstructor
public class RotationUpdateEvent extends Event {
    byte type;

    public boolean isPre() {
        return type == 0;
    }

    public boolean isPost() {
        return type == 1;
    }

    public static final byte PRE = 0;
    public static final byte POST = 1;
}
