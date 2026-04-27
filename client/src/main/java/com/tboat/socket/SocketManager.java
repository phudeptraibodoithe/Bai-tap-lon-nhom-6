package com.tboat.socket;

import javafx.application.Platform;
import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class SocketManager {
    private static SocketManager instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // Listener duy nhất để Controller hiện tại đăng ký nhận tin nhắn
    private Consumer<String> messageListener;

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
            System.out.println("[SocketManager] Đã kết nối đến Server: " + ip + ":" + port);

            // Bắt đầu lắng nghe tin nhắn từ Server
            startListening();
        }
    }


    public void setOnMessageReceived(Consumer<String> listener) {
        this.messageListener = listener;
    }

    private void startListening() {
        Thread thread = new Thread(() -> {
            try {
                String response;
                while (in != null && (response = in.readLine()) != null) {
                    final String msg = response;
                    System.out.println("[Server -> Client]: " + msg);

                    // Đẩy tin nhắn về luồng giao diện (JavaFX Application Thread)
                    if (messageListener != null) {
                        Platform.runLater(() -> messageListener.accept(msg));
                    }
                }
            } catch (IOException e) {
                System.err.println("[SocketManager] Mất kết nối với Server.");
                close();
            }
        });
        thread.setDaemon(true); // Tự động đóng luồng khi tắt App
        thread.start();
    }

    public void send(String msg) {
        if (out != null) {
            out.println(msg);
        } else {
            System.err.println("[SocketManager] Chưa kết nối, không thể gửi tin nhắn!");
        }
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }

    public void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            socket = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}