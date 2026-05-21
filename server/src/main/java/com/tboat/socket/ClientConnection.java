package com.tboat.socket;

import com.tboat.models.network.Response;
import com.tboat.models.network.ServerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

/**
 * Quản lý một kết nối socket của client.
 * Lớp này chỉ đọc dữ liệu thô, chuyển lệnh cho CommandRouter và dọn tài nguyên
 * khi client rời đi. Logic nghiệp vụ nằm ở các lớp handler và service.
 */
public class ClientConnection implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientConnection.class);

    private final Socket socket;
    private PrintWriter out;

    private final ClientSession context = new ClientSession();

    private final CommandRouter dispatcher = new CommandRouter(context);

    public ClientConnection(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            this.out = new PrintWriter(socket.getOutputStream(), true);

            context.setOut(out);
            EventBroadcaster.getInstance().register(context);
            context.sendResponse(new Response<>(ServerEvent.SERVER_READY, ServerEvent.SUCCESS,
                    "Chào mừng...", null));

            String line;
            while ((line = input.readLine()) != null) {
                dispatcher.dispatch(line.trim());
            }

        } catch (IOException e) {
            log.info("Kết nối với {} bị ngắt.", context.getClientId());
        } finally {
            cleanUp();
        }
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
