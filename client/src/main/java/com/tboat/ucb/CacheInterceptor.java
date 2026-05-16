package com.tboat.ucb;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.socket.SocketListener;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Một SocketListener đặc biệt — luôn lắng nghe TẤT CẢ response
 * và tự động lưu vào DataCache theo action.
 *
 * Đăng ký 1 lần duy nhất trong ClientApp, sau đó hoạt động ngầm mãi mãi.
 */
public class CacheInterceptor implements SocketListener {

    private static final Logger log = Logger.getLogger(CacheInterceptor.class.getName());
    private static CacheInterceptor instance;

    // Map message → action key để biết cache dưới key nào
    private static final Map<String, String> MESSAGE_TO_ACTION = Map.of(
            "Danh sách chờ duyệt",         "GET_PENDING_ITEMS",
            "Thông tin tài khoản",          "PROFILE",
            "Danh sách phiên đấu giá",      "LIST_AVAILABLE",   // ← TrangChu
            "Danh sách sản phẩm",           "LIST_AVAILABLE",   // ← fallback
            "Lịch sử",                      "GET_HISTORY",
            "Danh sách lượt đấu của bạn",   "GET_MY_AUCTIONS"
    );

    private CacheInterceptor() {}

    public static synchronized CacheInterceptor getInstance() {
        if (instance == null) instance = new CacheInterceptor();
        return instance;
    }

    @Override
    public void handleServerResponse(String response) {
        // Chạy trên background (không cần Platform.runLater vì chỉ update cache)
        try {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            String status  = json.has("status")  ? json.get("status").getAsString()  : "";
            String message = json.has("message") ? json.get("message").getAsString() : "";

            if (!"SUCCESS".equals(status)) return; // Chỉ cache khi thành công

            // Tìm action key tương ứng với message này
            String actionKey = null;
            for (Map.Entry<String, String> entry : MESSAGE_TO_ACTION.entrySet()) {
                if (message.contains(entry.getKey())) {
                    actionKey = entry.getValue();
                    break;
                }
            }

            if (actionKey != null) {
                DataCache.getInstance().put(actionKey, response);
                log.info("[CacheInterceptor] Cached: " + actionKey);
            }

        } catch (Exception e) {
            // Bỏ qua lỗi parse — đây chỉ là interceptor
        }
    }
}