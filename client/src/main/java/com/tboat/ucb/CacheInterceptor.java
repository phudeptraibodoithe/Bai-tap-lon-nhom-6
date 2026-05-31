package com.tboat.ucb;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.network.ServerEvent;
import com.tboat.socket.SocketListener;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Một SocketListener đặc biệt — luôn lắng nghe TẤT CẢ phản hồi
 * và tự động lưu vào DataCache theo action.
 *
 * Đăng ký 1 lần duy nhất trong ClientApp, sau đó hoạt động ngầm mãi mãi.
 */
public class CacheInterceptor implements SocketListener {

    private static final Logger log = Logger.getLogger(CacheInterceptor.class.getName());
    private static CacheInterceptor instance;

    // Bản đồ thông điệp sang khóa action để biết cache dưới khóa nào
    private static final Map<String, ServerEvent> MESSAGE_TO_ACTION = Map.of(
            "Danh sách chờ duyệt",         ServerEvent.GET_ALL_ITEMS,
            "Thông tin tài khoản",          ServerEvent.GET_PROFILE,
            "Danh sách phiên đấu giá",      ServerEvent.LIST_AVAILABLE,
            "Danh sách sản phẩm",           ServerEvent.LIST_AVAILABLE,
            "Lịch sử",                      ServerEvent.GET_HISTORY,
            "Danh sách lượt đấu của bạn",   ServerEvent.GET_MY_AUCTIONS
    );

    private CacheInterceptor() {}

    public static synchronized CacheInterceptor getInstance() {
        if (instance == null) instance = new CacheInterceptor();
        return instance;
    }

    @Override
    public void handleServerResponse(String response) {
        // Chạy trên luồng nền (không cần Platform.runLater vì chỉ cập nhật cache)
        try {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            String status  = json.has("status")  ? json.get("status").getAsString()  : "";
            String message = json.has("message") ? json.get("message").getAsString() : "";

            if (!ServerEvent.SUCCESS.name().equals(status)) return; // Chỉ cache khi thành công

            // Tìm action key tương ứng với thông điệp này
            ServerEvent actionKey = null;
            for (Map.Entry<String, ServerEvent> entry : MESSAGE_TO_ACTION.entrySet()) {
                if (message.contains(entry.getKey())) {
                    actionKey = entry.getValue();
                    break;
                }
            }

            if (actionKey != null) {
                DataCache.getInstance().put(actionKey, response);
                log.info("[CacheInterceptor] Cached: " + actionKey.name());
            }

        } catch (Exception e) {
            // Bỏ qua lỗi parse — đây chỉ là interceptor
        }
    }
}
