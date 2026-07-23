package dev.ethereal.api.event;

import dev.ethereal.api.event.orbit.ICancellable;

public class CancellableEvent implements ICancellable {
    private boolean cancelled;

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public void setCancel(boolean cancel) {
        this.cancelled = cancel;
    }

    public boolean isCancel() {
        return cancelled;
    }
}
