package com.tboat.socket;

import javafx.application.Platform;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class SocketManager {
    private static final Logger log = Logger.getLogger(SocketManager.class.getName());
    private static SocketManager instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final List<SocketListener> listeners = new CopyOnWriteArrayList<>();

    private SocketManager() {}

    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    public void connect(String ip, int port) throws IOException {
        if (socket == null || socket.isClosed()) {
            this.socket = new Socket(ip, port);
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            log.info("[SocketManager] Kết nối thành công đến " + ip + ":" + port);

            startListening();
        }
    }

    public void subscribe(SocketListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unsubscribe(SocketListener listener) {
        listeners.remove(listener);
    }

    private void startListening() {
        Thread thread = new Thread(() -> {
            try {
                String response;
                while (socket != null && !socket.isClosed() && (response = in.readLine()) != null) {
                    final String msg = response;
                    Platform.runLater(() -> {
                        for (SocketListener listener : listeners) {
                            listener.handleServerResponse(msg);
                        }
                    });
                }
            } catch (IOException e) {
                log.severe("[SocketManager] Mất kết nối server.");
                close();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void send(String msg) {
        if (out != null) {
            out.println(msg);
            out.flush();
        }
    }

    public void close() {
        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
            listeners.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}