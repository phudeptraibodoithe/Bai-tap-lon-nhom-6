package com.tboat.socket;

import com.google.gson.*;
import com.tboat.models.network.Response;
import com.tboat.service.AuctionRoom;
import com.tboat.service.NotificationService;
import com.tboat.service.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.time.LocalDateTime;

/**
 * ClientContext — "Hồ sơ" của một kết nối client.
 * Tập trung tất cả trạng thái của một client vào một chỗ:
 *   - clientId (tên tài khoản sau khi đăng nhập)
 *   - currentRoom (phòng đấu giá đang tham gia)
 *   - out (PrintWriter để gửi dữ liệu về socket)
 * Các Handler chỉ cần nhận context, không cần biết socket là gì.
 */
public class ClientContext {

    private static final Logger log = LoggerFactory.getLogger(ClientContext.class);

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src, t, ctx) -> new JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, t, ctx) -> LocalDateTime.parse(json.getAsString()))
            .create();

    private String clientId = "Guest";
    private AuctionRoom currentRoom;
    private PrintWriter out;

    // ── Getters / Setters ────────────────────────────────────────────────

    public String getClientId()               { return clientId; }
    public void   setClientId(String id)      { this.clientId = id; }

    public AuctionRoom getCurrentRoom()             { return currentRoom; }
    public void        setCurrentRoom(AuctionRoom r){ this.currentRoom = r; }

    public void setOut(PrintWriter out)        { this.out = out; }

    public boolean isGuest()                   { return "Guest".equals(clientId); }
    public boolean isLoggedIn()                { return !isGuest(); }

    // ── Gửi response ────────────────────────────────────────────────────

    public synchronized void sendResponse(Response<?> response) {
        if (out != null) {
            out.println(GSON.toJson(response));
            out.flush();
        }
    }

    public synchronized void sendSystemMessage(String action, String message, Object payload) {
        sendResponse(new Response<>(action, "SYSTEM", message, payload));
    }

    /** Tiện ích parse JSON — dùng chung trong tất cả Handler */
    public static Gson gson() { return GSON; }

    // ── Dọn dẹp ─────────────────────────────────────────────────────────

    public void cleanup() {
        GlobalBroadcaster.getInstance().unregister(this);
        if (isLoggedIn()) {
            UserManager.getInstance().logout(clientId);
            NotificationService.getInstance().unregister(clientId);
            log.info("[Server] Đã giải phóng tài nguyên cho user: {}", clientId);
        }
        if (currentRoom != null) {
            currentRoom.removeSubscriber(this); //truyền chính object này
            currentRoom = null;                 //tránh gọi lại lần 2
        }
    }
}
