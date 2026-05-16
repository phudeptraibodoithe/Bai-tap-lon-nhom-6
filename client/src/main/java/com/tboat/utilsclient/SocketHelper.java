package com.tboat.utilsclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.socket.SocketManager;
import com.tboat.utils.GsonUtils;

public class SocketHelper {

    // ======== PHẦN NHẬN DỮ LIỆU ========
    public static String getStatus(String jsonResponse) {
        try {
            JsonObject obj = JsonParser.parseString(jsonResponse).getAsJsonObject();
            return obj.has("status") ? obj.get("status").getAsString() : obj.get("action").getAsString();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    public static String getMessage(String jsonResponse) {
        try {
            JsonObject obj = JsonParser.parseString(jsonResponse).getAsJsonObject();
            return obj.has("message") ? obj.get("message").getAsString() : "Lỗi hệ thống";
        } catch (Exception e) {
            return "Lỗi phân tích dữ liệu";
        }
    }

    // ======== PHẦN GỬI DỮ LIỆU ========
    public static void sendRequest(String action, Object payload) {
        JsonObject request = new JsonObject();
        request.addProperty("action", action);

        if (payload != null) {
            if (payload instanceof Number) {
                request.addProperty("payload", (Number) payload);
            } else if (payload instanceof String) {
                request.addProperty("payload", (String) payload);
            } else if (payload instanceof Boolean) {
                request.addProperty("payload", (Boolean) payload);
            } else if (payload instanceof JsonElement) {
                // RẤT QUAN TRỌNG: Dùng .add() thay vì .addProperty() cho JsonObject
                request.add("payload", (JsonElement) payload);
            }
        }

        SocketManager.getInstance().send(GsonUtils.getInstance().toJson(request));
    }

    // Hàm Overload cho các request không cần payload (như PROFILE, LOGOUT)
    public static void sendRequest(String action) {
        sendRequest(action, null);
    }
}