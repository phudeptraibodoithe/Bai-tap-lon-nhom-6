package com.tboat.controllers.helper;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.socket.SocketListener;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * NotificationManager — nhận JSON từ server, giữ state trên client.
 *
 * Flow:
 *   Server (NotificationService.push) → socket → Client (SocketListener)
 *   → NotificationManager.receive() → ObservableList → HeaderController (UI)
 *
 * Gọi receive() từ SocketListener khi nhận action thuộc nhóm NOTIF_ACTIONS.
 */
public class NotificationManager implements SocketListener {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static final NotificationManager INSTANCE = new NotificationManager();
    private NotificationManager() {}
    public static NotificationManager getInstance() { return INSTANCE; }

    // ── Danh sách thông báo — HeaderController bọc FilteredList vào đây ───────
    private final ObservableList<NotificationItem> items =
            FXCollections.observableArrayList();

    public ObservableList<NotificationItem> getItems() { return items; }

    // ── Các action server gửi xuống được coi là thông báo ─────────────────────
    private static final java.util.Set<String> NOTIF_ACTIONS = java.util.Set.of(
            "NEW_BID", "OUTBID", "AUCTION_ENDING", "AUCTION_WON",
            "AUCTION_SOLD", "AUCTION_CANCELED", "ITEM_APPROVED", "ITEM_REJECTED",
            "DEPOSIT", "WITHDRAW"
    );

    // ════════════════════════════════════════════════════════════════════════
    // NHẬN DỮ LIỆU TỪ SERVER
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Gọi từ SocketListener khi nhận response từ server.
     * Tự lọc action — chỉ xử lý những action thuộc nhóm thông báo.
     *
     * <pre>
     * // SocketListener.java (hoặc ResponseHandler.java)
     * String action = response.getAction();
     * NotificationManager.getInstance().receive(action, response.getPayload());
     * // Các action khác (JOIN_SUCCESS, BID_RESULT...) xử lý riêng như cũ
     * </pre>
     *
     * @param action  action string từ Response (ví dụ "NEW_BID")
     * @param payload JsonObject chứa title, subtitle, avatarText, avatarColor
     */
    public void receive(String action, com.google.gson.JsonObject payload) {
        if (!NOTIF_ACTIONS.contains(action)) return;
        if (payload == null) return;
        if (!payload.has("title") || !payload.has("subtitle")) return;

        NotificationItem item = new NotificationItem(
                getStr(payload, "title",       action),
                getStr(payload, "subtitle",    ""),
                "Vừa xong",
                getStr(payload, "avatarText",  "📢"),
                getStr(payload, "avatarColor", "#E8F0FE")
        );

        // Luôn update UI trên FX thread — SocketListener thường chạy trên background thread
        if (Platform.isFxApplicationThread()) {
            items.add(0, item);
        } else {
            Platform.runLater(() -> items.add(0, item));
        }
    }

    @Override
    public void handleServerResponse(String response) {
        try {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            String action = json.has("type") && !json.get("type").isJsonNull()
                    ? json.get("type").getAsString()
                    : "";
            JsonObject payload = json.has("payload") && json.get("payload").isJsonObject()
                    ? json.getAsJsonObject("payload")
                    : null;
            if ("NOTIFICATION".equals(action) && payload != null && payload.has("notifType")) {
                action = payload.get("notifType").getAsString();
            }
            receive(action, payload);
        } catch (Exception ignored) {
            // Bỏ qua response không phải JSON thông báo.
        }
    }

    // ── Các thao tác khác ─────────────────────────────────────────────────────

    public void markAllRead() {
        items.forEach(NotificationItem::markRead);
    }

    public long getUnreadCount() {
        return items.stream().filter(n -> !n.isRead()).count();
    }

    public void addNotification(NotificationItem item) {
        items.add(0, item);
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private String getStr(com.google.gson.JsonObject obj, String key, String fallback) {
        return obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsString()
                : fallback;
    }

    // ════════════════════════════════════════════════════════════════════════
    // MODEL
    // ════════════════════════════════════════════════════════════════════════
    public static class NotificationItem {
        private final String  title;
        private final String  subtitle;
        private final String  time;
        private final String  avatarText;
        private final String  avatarColor;
        private       boolean read;

        public NotificationItem(String title, String subtitle, String time,
                                String avatarText, String avatarColor) {
            this.title       = title;
            this.subtitle    = subtitle;
            this.time        = time;
            this.avatarText  = avatarText;
            this.avatarColor = avatarColor;
            this.read        = false;
        }

        public String  getTitle()       { return title; }
        public String  getSubtitle()    { return subtitle; }
        public String  getTime()        { return time; }
        public String  getAvatarText()  { return avatarText; }
        public String  getAvatarColor() { return avatarColor; }
        public boolean isRead()         { return read; }
        public void    markRead()       { this.read = true; }
    }
}
