package com.tboat.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.socket.ClientSession;
import com.tboat.socket.CommandRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị cho CommandRouter.
 * KHÔNG cần DB vì test tầng định tuyến/xác thực trước khi chạm DAO.
 *
 * Lưu ý: Response tuần tự hóa trường tên là "type" (không phải "action").
 * Các hàm hỗ trợ đọc trường "type" từ JSON output.
 */
class CommandRouterTest {

    private ClientSession context;
    private StringWriter sw;
    private CommandRouter dispatcher;

    @BeforeEach
    void setUp() {
        context    = new ClientSession();
        sw         = new StringWriter();
        context.setOut(new PrintWriter(sw, true));
        dispatcher = new CommandRouter(context);
    }

    // ─── Hàm hỗ trợ ─────────────────────────────────────────────────────

    /** Trả về true nếu có ít nhất 1 dòng JSON trong output. */
    private boolean hasOutput() {
        return !sw.toString().trim().isEmpty();
    }

    /** Phân tích dòng JSON cuối cùng, trả về null nếu output rỗng hoặc phân tích lỗi. */
    private JsonObject lastResponseOrNull() {
        String raw = sw.toString().trim();
        if (raw.isEmpty()) return null;
        try {
            String[] lines = raw.split("\n");
            return JsonParser.parseString(lines[lines.length - 1].trim()).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    private JsonObject lastResponse() {
        JsonObject obj = lastResponseOrNull();
        assertNotNull(obj, "Dispatcher phải ghi ít nhất 1 response JSON vào output.");
        return obj;
    }

    /**
     * Đọc trường "type" từ Response (trường trong class Response là "type", không phải "action").
     */
    private String lastType() {
        JsonObject obj = lastResponse();
        assertTrue(obj.has("type") && !obj.get("type").isJsonNull(),
                "Response JSON phải có field 'type' không null. JSON thực tế: " + obj);
        return obj.get("type").getAsString();
    }

    private String lastStatus() {
        JsonObject obj = lastResponse();
        assertTrue(obj.has("status") && !obj.get("status").isJsonNull(),
                "Response JSON phải có field 'status' không null.");
        return obj.get("status").getAsString();
    }

    // ===================== GIÁ TRỊ TRẢ VỀ =====================

    @Test
    @DisplayName("dispatch trả về action string viết hoa từ JSON")
    void testDispatch_ReturnsActionUpperCase() {
        String returned = dispatcher.dispatch("{\"action\":\"login\"}");
        assertEquals("LOGIN", returned);
    }

    @Test
    @DisplayName("dispatch với JSON không có field 'action': trả về UNKNOWN")
    void testDispatch_NoActionField_ReturnsUnknown() {
        String returned = dispatcher.dispatch("{\"payload\":123}");
        assertEquals("UNKNOWN", returned);
    }

    // ===================== JSON KHÔNG HỢP LỆ =====================

    @Test
    @DisplayName("JSON rỗng → response type là ERROR_DISPATCH")
    void testDispatch_EmptyString_ErrorDispatch() {
        dispatcher.dispatch("");
        assertEquals("ERROR_DISPATCH", lastType());
    }

    @Test
    @DisplayName("JSON không hợp lệ → response type là ERROR_DISPATCH")
    void testDispatch_InvalidJson_ErrorDispatch() {
        dispatcher.dispatch("not json at all");
        assertEquals("ERROR_DISPATCH", lastType());
    }

    @Test
    @DisplayName("JSON null → response type là ERROR_DISPATCH, không throw")
    void testDispatch_NullInput_ErrorDispatch() {
        assertDoesNotThrow(() -> dispatcher.dispatch(null));
        assertEquals("ERROR_DISPATCH", lastType());
    }

    @Test
    @DisplayName("JSON thiếu dấu đóng → ERROR_DISPATCH")
    void testDispatch_MalformedJson_ErrorDispatch() {
        dispatcher.dispatch("{\"action\":\"LOGIN\"");
        assertEquals("ERROR_DISPATCH", lastType());
    }

    // ===================== ACTION KHÔNG TỒN TẠI =====================

    @Test
    @DisplayName("Action không tồn tại khi đã login → UNKNOWN, status ERROR")
    void testDispatch_UnknownAction_WhenLoggedIn() {
        context.setClientId("alice");
        dispatcher.dispatch("{\"action\":\"NONEXISTENT_CMD\"}");
        assertEquals("UNKNOWN", lastType());
        assertEquals("ERROR", lastStatus());
    }

    @Test
    @DisplayName("Action không tồn tại: message phải chứa tên action")
    void testDispatch_UnknownAction_MessageContainsActionName() {
        context.setClientId("alice");
        dispatcher.dispatch("{\"action\":\"FAKE_CMD\"}");
        String message = lastResponse().get("message").getAsString();
        assertTrue(message.contains("FAKE_CMD"));
    }

    // ===================== KIỂM SOÁT TRUY CẬP CỦA KHÁCH =====================
    // Các action bị chặn ở chốt xác thực → phản hồi luôn được ghi ngay

    @Test
    @DisplayName("Guest gọi BID → AUTH_ERROR")
    void testDispatch_Guest_BID_AuthError() {
        dispatcher.dispatch("{\"action\":\"BID\",\"payload\":1000}");
        assertEquals("AUTH_ERROR", lastType());
        assertEquals("ERROR", lastStatus());
    }

    @Test
    @DisplayName("Guest gọi JOIN → AUTH_ERROR")
    void testDispatch_Guest_JOIN_AuthError() {
        dispatcher.dispatch("{\"action\":\"JOIN\",\"payload\":1}");
        assertEquals("AUTH_ERROR", lastType());
    }

    @Test
    @DisplayName("Guest gọi PROFILE → AUTH_ERROR")
    void testDispatch_Guest_PROFILE_AuthError() {
        dispatcher.dispatch("{\"action\":\"PROFILE\"}");
        assertEquals("AUTH_ERROR", lastType());
    }

    @Test
    @DisplayName("Guest gọi LOGOUT → AUTH_ERROR")
    void testDispatch_Guest_LOGOUT_AuthError() {
        dispatcher.dispatch("{\"action\":\"LOGOUT\"}");
        assertEquals("AUTH_ERROR", lastType());
    }

    @Test
    @DisplayName("Guest gọi GET_HISTORY → AUTH_ERROR")
    void testDispatch_Guest_GET_HISTORY_AuthError() {
        dispatcher.dispatch("{\"action\":\"GET_HISTORY\"}");
        assertEquals("AUTH_ERROR", lastType());
    }

    @Test
    @DisplayName("Guest gọi TRANSACTION → AUTH_ERROR")
    void testDispatch_Guest_TRANSACTION_AuthError() {
        dispatcher.dispatch("{\"action\":\"TRANSACTION\",\"payload\":1000}");
        assertEquals("AUTH_ERROR", lastType());
    }

    @Test
    @DisplayName("Guest gọi POST_ITEM → AUTH_ERROR")
    void testDispatch_Guest_POST_ITEM_AuthError() {
        dispatcher.dispatch("{\"action\":\"POST_ITEM\",\"payload\":{}}");
        assertEquals("AUTH_ERROR", lastType());
    }

    @Test
    @DisplayName("Guest gọi CANCEL_AUCTION → AUTH_ERROR")
    void testDispatch_Guest_CANCEL_AUCTION_AuthError() {
        dispatcher.dispatch("{\"action\":\"CANCEL_AUCTION\",\"payload\":1}");
        assertEquals("AUTH_ERROR", lastType());
    }

    @Test
    @DisplayName("Guest gọi EDIT_ITEM → AUTH_ERROR")
    void testDispatch_Guest_EDIT_ITEM_AuthError() {
        dispatcher.dispatch("{\"action\":\"EDIT_ITEM\",\"payload\":{}}");
        assertEquals("AUTH_ERROR", lastType());
    }

    @Test
    @DisplayName("Guest gọi GET_MY_AUCTIONS → AUTH_ERROR")
    void testDispatch_Guest_GET_MY_AUCTIONS_AuthError() {
        dispatcher.dispatch("{\"action\":\"GET_MY_AUCTIONS\"}");
        assertEquals("AUTH_ERROR", lastType());
    }

    @Test
    @DisplayName("Guest gọi GET_SESSION_BIDS → AUTH_ERROR")
    void testDispatch_Guest_GET_SESSION_BIDS_AuthError() {
        dispatcher.dispatch("{\"action\":\"GET_SESSION_BIDS\",\"payload\":1}");
        assertEquals("AUTH_ERROR", lastType());
    }

    // ===================== ACTION CÔNG KHAI (KHÁCH ĐƯỢC PHÉP) =====================
    // Vượt qua chốt xác thực, chỉ kiểm tra KHÔNG phải AUTH_ERROR

    @Test
    @DisplayName("Guest gọi LIST_AVAILABLE → KHÔNG phải AUTH_ERROR")
    void testDispatch_Guest_LIST_AVAILABLE_Allowed() {
        dispatcher.dispatch("{\"action\":\"LIST_AVAILABLE\"}");
        if (hasOutput()) {
            assertNotEquals("AUTH_ERROR", lastType());
        }
    }

    @Test
    @DisplayName("Guest gọi LOGIN → KHÔNG phải AUTH_ERROR")
    void testDispatch_Guest_LOGIN_Allowed() {
        dispatcher.dispatch("{\"action\":\"LOGIN\"}");
        if (hasOutput()) {
            assertNotEquals("AUTH_ERROR", lastType());
        }
    }

    @Test
    @DisplayName("Guest gọi REGISTER → KHÔNG phải AUTH_ERROR")
    void testDispatch_Guest_REGISTER_Allowed() {
        dispatcher.dispatch("{\"action\":\"REGISTER\"}");
        if (hasOutput()) {
            assertNotEquals("AUTH_ERROR", lastType());
        }
    }

    // ===================== KIỂM SOÁT TRUY CẬP ADMIN =====================
    // ADMIN_ACTIONS = ["GET_ALL_ITEMS", "APPROVE_ITEM", "REJECT_ITEM"]
    // Bị chặn trước khi chạm DB nên phản hồi luôn được ghi

    @Test
    @DisplayName("User thường gọi APPROVE_ITEM → AUTH_ERROR")
    void testDispatch_NonAdmin_APPROVE_ITEM_AuthError() {
        context.setClientId("alice");
        dispatcher.dispatch("{\"action\":\"APPROVE_ITEM\",\"payload\":1}");
        assertEquals("AUTH_ERROR", lastType());
    }

    @Test
    @DisplayName("User thường gọi REJECT_ITEM → AUTH_ERROR")
    void testDispatch_NonAdmin_REJECT_ITEM_AuthError() {
        context.setClientId("bob");
        dispatcher.dispatch("{\"action\":\"REJECT_ITEM\",\"payload\":1}");
        assertEquals("AUTH_ERROR", lastType());
    }

    @Test
    @DisplayName("User thường gọi GET_ALL_ITEMS → AUTH_ERROR")
    void testDispatch_NonAdmin_GET_ALL_ITEMS_AuthError() {
        context.setClientId("charlie");
        dispatcher.dispatch("{\"action\":\"GET_ALL_ITEMS\"}");
        assertEquals("AUTH_ERROR", lastType());
    }

    @Test
    @DisplayName("Guest gọi APPROVE_ITEM → AUTH_ERROR (guest check trước admin check)")
    void testDispatch_Guest_APPROVE_ITEM_AuthError() {
        dispatcher.dispatch("{\"action\":\"APPROVE_ITEM\",\"payload\":1}");
        assertEquals("AUTH_ERROR", lastType());
    }

    @Test
    @DisplayName("Admin gọi APPROVE_ITEM → KHÔNG phải AUTH_ERROR")
    void testDispatch_Admin_APPROVE_ITEM_Allowed() {
        context.setClientId("admin");
        dispatcher.dispatch("{\"action\":\"APPROVE_ITEM\",\"payload\":1}");
        if (hasOutput()) {
            assertNotEquals("AUTH_ERROR", lastType());
        }
    }

    @Test
    @DisplayName("Admin gọi GET_ALL_ITEMS → KHÔNG phải AUTH_ERROR")
    void testDispatch_Admin_GET_ALL_ITEMS_Allowed() {
        context.setClientId("admin");
        dispatcher.dispatch("{\"action\":\"GET_ALL_ITEMS\"}");
        if (hasOutput()) {
            assertNotEquals("AUTH_ERROR", lastType());
        }
    }

    @Test
    @DisplayName("Admin gọi REJECT_ITEM → KHÔNG phải AUTH_ERROR")
    void testDispatch_Admin_REJECT_ITEM_Allowed() {
        context.setClientId("admin");
        dispatcher.dispatch("{\"action\":\"REJECT_ITEM\",\"payload\":1}");
        if (hasOutput()) {
            assertNotEquals("AUTH_ERROR", lastType());
        }
    }

    // ===================== KHÔNG PHÂN BIỆT HOA THƯỜNG =====================

    @Test
    @DisplayName("action viết thường 'bid' (guest) → AUTH_ERROR (normalize uppercase)")
    void testDispatch_LowerCaseAction_NormalizedToUpperCase() {
        dispatcher.dispatch("{\"action\":\"bid\",\"payload\":1000}");
        assertEquals("AUTH_ERROR", lastType());
    }

    @Test
    @DisplayName("action 'LiSt_AvAiLaBlE' → được route đúng (không phải AUTH_ERROR/UNKNOWN)")
    void testDispatch_MixedCaseAction_Normalized() {
        dispatcher.dispatch("{\"action\":\"LiSt_AvAiLaBlE\"}");
        if (hasOutput()) {
            assertNotEquals("AUTH_ERROR", lastType());
            assertNotEquals("UNKNOWN", lastType());
        }
    }

    // ===================== USER ĐÃ ĐĂNG NHẬP =====================

    @Test
    @DisplayName("User đã login gọi BID khi chưa trong phòng → BID (không AUTH_ERROR)")
    void testDispatch_LoggedIn_BID_NotInRoom() {
        context.setClientId("alice");
        dispatcher.dispatch("{\"action\":\"BID\",\"payload\":5000}");
        // Handler BID ghi phản hồi ngay khi không trong phòng (không cần DB)
        assertEquals("BID", lastType());
        assertNotEquals("AUTH_ERROR", lastType());
    }

    @Test
    @DisplayName("User đã login gọi PROFILE → không AUTH_ERROR")
    void testDispatch_LoggedIn_PROFILE_Routed() {
        context.setClientId("alice");
        dispatcher.dispatch("{\"action\":\"PROFILE\"}");
        if (hasOutput()) {
            assertNotEquals("AUTH_ERROR", lastType());
        }
    }

    @Test
    @DisplayName("User đã login gọi LOGOUT → không AUTH_ERROR")
    void testDispatch_LoggedIn_LOGOUT_Allowed() {
        context.setClientId("alice");
        dispatcher.dispatch("{\"action\":\"LOGOUT\"}");
        if (hasOutput()) {
            assertNotEquals("AUTH_ERROR", lastType());
        }
    }

    // ===================== PHẢN HỒI ĐỦ TRƯỜNG =====================

    @Test
    @DisplayName("AUTH_ERROR response phải có đủ type, status, message")
    void testDispatch_AuthError_HasRequiredFields() {
        dispatcher.dispatch("{\"action\":\"BID\",\"payload\":1000}");
        JsonObject resp = lastResponse();
        assertTrue(resp.has("type"),    "Response phải có field 'type'.");
        assertTrue(resp.has("status"),  "Response phải có field 'status'.");
        assertTrue(resp.has("message"), "Response phải có field 'message'.");
    }

    @Test
    @DisplayName("AUTH_ERROR response: status phải là ERROR")
    void testDispatch_AuthError_StatusIsError() {
        dispatcher.dispatch("{\"action\":\"BID\",\"payload\":1000}");
        assertEquals("ERROR", lastStatus());
    }

    @Test
    @DisplayName("ERROR_DISPATCH response: status phải là ERROR")
    void testDispatch_ErrorDispatch_StatusIsError() {
        dispatcher.dispatch("bad input");
        assertEquals("ERROR", lastStatus());
    }

    @Test
    @DisplayName("UNKNOWN action response: status phải là ERROR")
    void testDispatch_UnknownAction_StatusIsError() {
        context.setClientId("alice");
        dispatcher.dispatch("{\"action\":\"FAKE_CMD\"}");
        assertEquals("ERROR", lastStatus());
    }
}
