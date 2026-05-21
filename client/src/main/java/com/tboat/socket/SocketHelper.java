package com.tboat.socket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.network.ServerEvent;
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

    public static String getType(String jsonResponse) {
        try {
            JsonObject obj = JsonParser.parseString(jsonResponse).getAsJsonObject();
            return obj.has("type") ? obj.get("type").getAsString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    public static JsonArray getPayloadArray(String jsonResponse) {
        try {
            JsonObject obj = JsonParser.parseString(jsonResponse).getAsJsonObject();
            if (obj.has("payload") && obj.get("payload").isJsonArray()) {
                return obj.getAsJsonArray("payload");
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static JsonObject getPayloadObject(String jsonResponse) {
        try {
            JsonObject obj = JsonParser.parseString(jsonResponse).getAsJsonObject();
            if (obj.has("payload") && !obj.get("payload").isJsonNull()
                    && obj.get("payload").isJsonObject()) {
                return obj.getAsJsonObject("payload");
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // Hàm Overload cho các request không cần payload (như PROFILE, LOGOUT)
    public static void sendRequest(String action) {
        sendRequest(action, null);
    }

    // Trong SocketHelper.java
    public static ServerEvent getTypeEnum(String response) {
        try {
            return ServerEvent.valueOf(getType(response));
        } catch (IllegalArgumentException e) {
            return ServerEvent.UNKNOWN;
        }
    }

    public static ServerEvent getStatusEnum(String response) {
        try {
            return ServerEvent.valueOf(getStatus(response));
        } catch (IllegalArgumentException e) {
            return ServerEvent.UNKNOWN;
        }
    }
}