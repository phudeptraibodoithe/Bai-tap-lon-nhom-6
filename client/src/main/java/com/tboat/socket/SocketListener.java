package com.tboat.socket;

public interface SocketListener {
    void handleServerResponse(String response);

    default void registerSocket() {
        SocketManager.getInstance().subscribe(this);
    }

    default void unregisterSocket() {
        SocketManager.getInstance().unsubscribe(this);
    }
}