package com.tboat.socket;

import com.google.gson.*;
import com.tboat.models.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

/**
 * ClientHandler — Chỉ chịu trách nhiệm:
 *  1. Đọc dữ liệu thô từ socket
 *  2. Uỷ quyền xử lý cho CommandDispatcher
 *  3. Gửi response về client
 *  4. Dọn dẹp tài nguyên khi ngắt kết nối
 * KHÔNG chứa bất kỳ business logic nào.
 */
public class ClientHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket socket;
    private PrintWriter out;

    // Context giữ trạng thái của một client (thay thế các field rải rác cũ)
    private final ClientContext context = new ClientContext();

    // Dispatcher chịu trách nhiệm routing
    private final CommandDispatcher dispatcher = new CommandDispatcher(context);

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            this.out = new PrintWriter(socket.getOutputStream(), true);

            // Gắn output vào context để các handler có thể gửi response
            context.setOut(out);

            sendResponse(new Response<>("SERVER_READY", "SUCCESS",
                    "Chào mừng bạn đến với hệ thống đấu giá TBoat!", null));

            String line;
            while ((line = in.readLine()) != null) {
                String result = dispatcher.dispatch(line.trim());
                // dispatcher tự gọi context.sendResponse() bên trong
                // result chỉ dùng để log nếu cần
            }

        } catch (IOException e) {
            log.info("Kết nối với {} bị ngắt.", context.getClientId());
        } finally {
            cleanUp();
        }
    }

    public synchronized void sendResponse(Response<?> response) {
        context.sendResponse(response);
    }

    private void cleanUp() {
        context.cleanup();
        try {
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            log.error("Lỗi đóng socket: {}", e.getMessage());
        }
    }
}
