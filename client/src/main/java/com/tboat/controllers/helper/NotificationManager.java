package com.tboat.controllers.helper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.network.ServerEvent;
import com.tboat.session.UserSession;
import com.tboat.socket.SocketHelper;
import com.tboat.socket.SocketListener;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.Set;
import java.util.prefs.Preferences;
/**
 * NotificationManager — nhận JSON từ server, giữ trạng thái trên client.
 *
 * Luồng xử lý:
 *   Máy chủ (NotificationService.push) → socket → máy khách (SocketListener)
 *   → NotificationManager.receive() → ObservableList → HeaderController (UI)
 *
 * Gọi receive() từ SocketListener khi nhận action thuộc nhóm NOTIF_ACTIONS.
 */
public class NotificationManager implements SocketListener {

    // ── Singleton dùng chung toàn ứng dụng ───────────────────────────────────
    private static final NotificationManager INSTANCE = new NotificationManager();
    private NotificationManager() {}
    public static NotificationManager getInstance() { return INSTANCE; }
    private static final Preferences PREFS = Preferences.userNodeForPackage(NotificationManager.class);
    private static final String PREFS_PREFIX = "notifications.";
    private static final int MAX_STORED_ITEMS = 50;
    private String activeAccount;

    // ── Danh sách thông báo — HeaderController bọc FilteredList vào đây ───────
    private final ObservableList<NotificationItem> items =
            FXCollections.observableArrayList();

    public ObservableList<NotificationItem> getItems() { return items; }

    public synchronized void useAccount(String account) {
        if (account == null || account.isBlank() || account.equals(activeAccount)) return;
        saveCurrent();
        activeAccount = account;
        items.setAll(load(account));
    }

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
     * Gọi từ SocketListener khi nhận phản hồi từ server.
     * Tự lọc action — chỉ xử lý những action thuộc nhóm thông báo.
     *
     * <pre>
     * // SocketListener.java (hoặc bộ xử lý phản hồi)
     * String action = response.getAction();
     * NotificationManager.getInstance().receive(action, response.getPayload());
     * // Các action khác (JOIN_SUCCESS, BID_RESULT...) xử lý riêng như cũ
     * </pre>
     *
     * @param event  action dạng chuỗi từ Response (ví dụ "NEW_BID")
     * @param payload JsonObject chứa tiêu đề, phụ đề, chữ đại diện và màu đại diện
     */
    public void receive(ServerEvent event, JsonObject payload) {
        if (!NOTIF_ACTIONS.contains(event)) return;
        if (payload == null) return;
        if (!payload.has("title") || !payload.has("subtitle")) return;
        ensureActiveAccount();

        NotificationItem item = new NotificationItem(
                getStr(payload, "title",       event.name()),
                getStr(payload, "subtitle",    ""),
                "Vừa xong",
                getStr(payload, "avatarText",  "📢"),
                getStr(payload, "avatarColor", "#E8F0FE")
        );

        // Luôn cập nhật UI trên luồng FX vì SocketListener thường chạy trên luồng nền
        if (Platform.isFxApplicationThread()) {
            addItem(item);
        } else {
            Platform.runLater(() -> addItem(item));
        }
    }

    // handleServerResponse cũng cần cập nhật theo:
    @Override
    public void handleServerResponse(String response) {
        try {
            JsonObject json    = JsonParser.parseString(response).getAsJsonObject();
            ServerEvent type   = SocketHelper.getTypeEnum(response);
            JsonObject payload = SocketHelper.getPayloadObject(response);

            // Nếu là gói bọc NOTIFICATION thì đọc notifType bên trong
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
        saveCurrent();
    }

    public long getUnreadCount() {
        return items.stream().filter(n -> !n.isRead()).count();
    }

    public void addNotification(NotificationItem item) {
        addItem(item);
    }

    // ── Hàm hỗ trợ ───────────────────────────────────────────────────────────
    private void addItem(NotificationItem item) {
        items.add(0, item);
        while (items.size() > MAX_STORED_ITEMS) {
            items.remove(items.size() - 1);
        }
        saveCurrent();
    }

    private void ensureActiveAccount() {
        if (activeAccount == null) {
            useAccount(UserSession.getInstance().getUsername());
        }
    }

    private synchronized void saveCurrent() {
        if (activeAccount == null || activeAccount.isBlank()) return;

        JsonArray arr = new JsonArray();
        int count = Math.min(items.size(), MAX_STORED_ITEMS);
        for (int i = 0; i < count; i++) {
            NotificationItem item = items.get(i);
            JsonObject obj = new JsonObject();
            obj.addProperty("title", item.title);
            obj.addProperty("subtitle", item.subtitle);
            obj.addProperty("time", item.time);
            obj.addProperty("avatarText", item.avatarText);
            obj.addProperty("avatarColor", item.avatarColor);
            obj.addProperty("read", item.read);
            arr.add(obj);
        }
        while (arr.toString().length() > Preferences.MAX_VALUE_LENGTH && arr.size() > 0) {
            arr.remove(arr.size() - 1);
        }
        PREFS.put(storageKey(activeAccount), arr.toString());
    }

    private ObservableList<NotificationItem> load(String account) {
        ObservableList<NotificationItem> loaded = FXCollections.observableArrayList();
        String raw = PREFS.get(storageKey(account), "[]");
        try {
            JsonArray arr = JsonParser.parseString(raw).getAsJsonArray();
            for (JsonElement element : arr) {
                JsonObject obj = element.getAsJsonObject();
                NotificationItem item = new NotificationItem(
                        getStr(obj, "title", ""),
                        getStr(obj, "subtitle", ""),
                        getStr(obj, "time", "Vừa xong"),
                        getStr(obj, "avatarText", "📢"),
                        getStr(obj, "avatarColor", "#E8F0FE")
                );
                item.read = obj.has("read") && obj.get("read").getAsBoolean();
                loaded.add(item);
            }
        } catch (Exception ignored) {
            PREFS.remove(storageKey(account));
        }
        return loaded;
    }

    private String storageKey(String account) {
        return PREFS_PREFIX + account.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }

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
        public void    markRead()       {
            if (!read) {
                this.read = true;
                NotificationManager.getInstance().saveCurrent();
            }
        }
    }
}
