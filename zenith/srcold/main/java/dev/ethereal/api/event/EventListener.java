package dev.ethereal.api.event;

public record EventListener(Runnable action) {
    public void unsubscribe() {
        action.run();
    }
}
