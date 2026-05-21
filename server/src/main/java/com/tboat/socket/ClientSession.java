package com.tboat.socket;

import com.google.gson.*;
import com.tboat.models.network.Response;
import com.tboat.models.network.ServerEvent;
import com.tboat.service.AuctionRoom;
import com.tboat.service.NotificationService;
import com.tboat.service.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.time.LocalDateTime;

/**
 * Lưu trạng thái của một kết nối client.
 * Các lớp handler dùng object này để đọc tài khoản đã đăng nhập, phòng hiện tại
 * và writer của socket mà không cần chạm trực tiếp vào socket thô.
 */
public class ClientSession {

    private static final Logger log = LoggerFactory.getLogger(ClientSession.class);

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (source, type, client) ->
                            new JsonPrimitive(source.toString()))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, type, client) ->
                            LocalDateTime.parse(json.getAsString()))
            .create();

    private String clientId = "Guest";
    private AuctionRoom currentRoom;
    private PrintWriter out;

    public String getClientId()               { return clientId; }
    public void   setClientId(String clientId){ this.clientId = clientId; }

    public AuctionRoom getCurrentRoom()             { return currentRoom; }
    public void        setCurrentRoom(AuctionRoom room){ this.currentRoom = room; }

    public void setOut(PrintWriter out)        { this.out = out; }

    public boolean isGuest()                   { return "Guest".equals(clientId); }
    public boolean isLoggedIn()                { return !isGuest(); }

    public synchronized void sendResponse(Response<?> response) {
        if (out != null) {
            out.println(GSON.toJson(response));
            out.flush();
        }
    }

    public synchronized void sendSystemMessage(String action, String message, Object payload) {
        sendResponse(new Response<>(action, "SYSTEM", message, payload));
    }

    public synchronized void sendSystemMessage(ServerEvent action, String message, Object payload) {
        sendResponse(new Response<>(action, ServerEvent.SYSTEM, message, payload));
    }

    public static Gson gson() { return GSON; }

    public void cleanup() {
        EventBroadcaster.getInstance().unregister(this);
        if (isLoggedIn()) {
            UserManager.getInstance().logout(clientId);
            NotificationService.getInstance().unregister(clientId);
            log.info("[Server] Đã giải phóng tài nguyên cho user: {}", clientId);
        }
        if (currentRoom != null) {
            currentRoom.removeSubscriber(this);
            currentRoom = null;
        }
    }
}
