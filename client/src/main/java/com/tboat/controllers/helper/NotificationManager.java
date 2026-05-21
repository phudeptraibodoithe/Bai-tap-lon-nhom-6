package com.tboat.controllers.helper;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.network.ServerEvent;
import com.tboat.socket.SocketHelper;
import com.tboat.socket.SocketListener;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.Set;
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
    private static final Set<ServerEvent> NOTIF_ACTIONS = Set.of(
            ServerEvent.NEW_BID,
            ServerEvent.OUTBID,
            ServerEvent.AUCTION_STARTED,
            ServerEvent.AUCTION_ENDING,
            ServerEvent.AUCTION_CANCELED,
            ServerEvent.AUCTION_FINISHED,
            ServerEvent.AUCTION_WON,
            ServerEvent.AUCTION_SOLD,
            ServerEvent.ITEM_APPROVED,
            ServerEvent.ITEM_REJECTED,
            ServerEvent.DEPOSIT,
            ServerEvent.WITHDRAW,
            ServerEvent.TRANSACTION,
            ServerEvent.APPROVE_ITEM,
            ServerEvent.REJECT_ITEM
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
     * @param event  action string từ Response (ví dụ "NEW_BID")
     * @param payload JsonObject chứa title, subtitle, avatarText, avatarColor
     */
    public void receive(ServerEvent event, JsonObject payload) {
        if (!NOTIF_ACTIONS.contains(event)) return;
        if (payload == null) return;
        if (!payload.has("title") || !payload.has("subtitle")) return;

        NotificationItem item = new NotificationItem(
                getStr(payload, "title",       event.name()),
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

    // handleServerResponse cũng cần cập nhật theo:
    @Override
    public void handleServerResponse(String response) {
        try {
            JsonObject json    = JsonParser.parseString(response).getAsJsonObject();
            ServerEvent type   = SocketHelper.getTypeEnum(response);
            JsonObject payload = SocketHelper.getPayloadObject(response);

            // Nếu là NOTIFICATION wrapper → đọc notifType bên trong
            if (type == ServerEvent.NOTIFICATION && payload != null
                    && payload.has("notifType")) {
                try {
                    type = ServerEvent.valueOf(payload.get("notifType").getAsString());
                } catch (IllegalArgumentException ignored) {
                    return;
                }
            }
            receive(type, payload);
        } catch (Exception ignored) {}
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
    private String getStr(JsonObject obj, String key, String fallback) {
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
