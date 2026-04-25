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

    // Listener để các Controller đăng ký nhận tin nhắn
    private Consumer<String> messageListener;

    private SocketManager() {}

    public static SocketManager getInstance() {
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
            System.out.println("DA KET NOI DEN SERVER: " + ip);

            // KHỞI CHẠY LUỒNG LẮNG NGHE DUY NHẤT CHO TOÀN APP
            startListening();
        }
    }

    /**
     * Mỗi Controller khi mở lên sẽ gọi hàm này để "đăng ký" nhận tin nhắn.
     * Khi gọi hàm này, Listener cũ sẽ bị ghi đè, giúp tin nhắn chỉ gửi đến màn hình hiện tại.
     */
    public void setOnMessageReceived(Consumer<String> listener) {
        this.messageListener = listener;
    }

    private void startListening() {
        Thread thread = new Thread(() -> {
            try {
                String response;
                // Chỉ có DUY NHẤT luồng này được phép gọi readLine()
                while (in != null && (response = in.readLine()) != null) {
                    final String msg = response;
                    System.out.println("[Server -> Client]: " + msg);

                    // Gửi tin nhắn về cho Controller đang đăng ký xử lý (trên UI Thread)
                    if (messageListener != null) {
                        Platform.runLater(() -> messageListener.accept(msg));
                    }
                }
            } catch (IOException e) {
                System.err.println("Mất kết nối với Server.");
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Hàm gửi tin nhắn tập trung
     */
    public void send(String msg) {
        if (out != null) {
            out.println(msg);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}