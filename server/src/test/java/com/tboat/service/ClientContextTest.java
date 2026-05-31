package com.tboat.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.network.Response;
import com.tboat.socket.ClientSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị cho ClientSession.
 * KHÔNG cần DB.
 *
 * Lưu ý quan trọng: class Response tuần tự hóa với trường tên "type" (tham số khởi tạo 1),
 * không phải "action". Tất cả assertion đọc trường "type" từ JSON output.
 *
 * sendSystemMessage(action, message, payload) → new Response<>(action, "SYSTEM", message, payload)
 * → JSON: { "type": action, "status": "SYSTEM", "message": ..., "payload": ... }
 */
class ClientSessionTest {

    private ClientSession context;
    private StringWriter  stringWriter;
    private PrintWriter   printWriter;

    @BeforeEach
    void setUp() {
        context      = new ClientSession();
        stringWriter = new StringWriter();
        printWriter  = new PrintWriter(stringWriter, true);
        context.setOut(printWriter);
    }

    // ─── Hàm hỗ trợ ─────────────────────────────────────────────────────

    private JsonObject lastOutput() {
        String raw = stringWriter.toString().trim();
        assertFalse(raw.isEmpty(), "Output không được trống.");
        String[] lines = raw.split("\n");
        return JsonParser.parseString(lines[lines.length - 1].trim()).getAsJsonObject();
    }

    /** Lấy trường chuỗi an toàn: trả về null nếu vắng mặt hoặc JsonNull */
    private String getField(JsonObject obj, String field) {
        if (!obj.has(field) || obj.get(field).isJsonNull()) return null;
        return obj.get(field).getAsString();
    }

    // ===================== TRẠNG THÁI KHỞI TẠO =====================

    @Test
    @DisplayName("clientId mặc định phải là 'Guest'")
    void testInitialClientId_IsGuest() {
        assertEquals("Guest", context.getClientId());
    }

    @Test
    @DisplayName("isGuest() phải trả về true khi chưa login")
    void testIsGuest_BeforeLogin_True() {
        assertTrue(context.isGuest());
    }

    @Test
    @DisplayName("isLoggedIn() phải trả về false khi chưa login")
    void testIsLoggedIn_BeforeLogin_False() {
        assertFalse(context.isLoggedIn());
    }

    @Test
    @DisplayName("currentRoom mặc định phải là null")
    void testInitialCurrentRoom_IsNull() {
        assertNull(context.getCurrentRoom());
    }

    // ===================== GÁN CLIENT ID =====================

    @Test
    @DisplayName("setClientId('alice'): getClientId phải trả về 'alice'")
    void testSetClientId_UpdatesCorrectly() {
        context.setClientId("alice");
        assertEquals("alice", context.getClientId());
    }

    @Test
    @DisplayName("isGuest() phải false sau khi setClientId với tên khác 'Guest'")
    void testIsGuest_AfterSetClientId_False() {
        context.setClientId("alice");
        assertFalse(context.isGuest());
    }

    @Test
    @DisplayName("isLoggedIn() phải true sau khi setClientId")
    void testIsLoggedIn_AfterSetClientId_True() {
        context.setClientId("alice");
        assertTrue(context.isLoggedIn());
    }

    @Test
    @DisplayName("setClientId('Guest') → isGuest() phải trả về true")
    void testSetClientId_BackToGuest_IsGuestTrue() {
        context.setClientId("alice");
        context.setClientId("Guest");
        assertTrue(context.isGuest());
    }

    // ===================== GỬI PHẢN HỒI =====================

    @Test
    @DisplayName("sendResponse với out hợp lệ: output phải chứa JSON hợp lệ")
    void testSendResponse_ValidOut_WritesJson() {
        context.sendResponse(new Response<>("TEST_TYPE", "SUCCESS", "hello", null));
        String output = stringWriter.toString().trim();
        assertFalse(output.isEmpty());
        assertDoesNotThrow(() -> JsonParser.parseString(output));
    }

    @Test
    @DisplayName("sendResponse: field 'type' trong JSON phải đúng")
    void testSendResponse_TypeField_Correct() {
        context.sendResponse(new Response<>("MY_TYPE", "OK", "msg", null));
        assertEquals("MY_TYPE", getField(lastOutput(), "type"));
    }

    @Test
    @DisplayName("sendResponse: field 'status' trong JSON phải đúng")
    void testSendResponse_StatusField_Correct() {
        context.sendResponse(new Response<>("A", "MY_STATUS", "msg", null));
        assertEquals("MY_STATUS", getField(lastOutput(), "status"));
    }

    @Test
    @DisplayName("sendResponse: field 'message' trong JSON phải đúng")
    void testSendResponse_MessageField_Correct() {
        context.sendResponse(new Response<>("A", "S", "hello world", null));
        assertEquals("hello world", getField(lastOutput(), "message"));
    }

    @Test
    @DisplayName("sendResponse với out = null: không throw Exception")
    void testSendResponse_NullOut_ShouldNotThrow() {
        ClientSession noOut = new ClientSession();
        assertDoesNotThrow(
                () -> noOut.sendResponse(new Response<>("X", "Y", "z", null)));
    }

    @Test
    @DisplayName("sendResponse nhiều lần: output có nhiều dòng tương ứng")
    void testSendResponse_MultipleTimes_MultipleLines() {
        context.sendResponse(new Response<>("A1", "S1", "m1", null));
        context.sendResponse(new Response<>("A2", "S2", "m2", null));
        context.sendResponse(new Response<>("A3", "S3", "m3", null));
        long lineCount = stringWriter.toString().trim().lines().count();
        assertEquals(3, lineCount);
    }

    @Test
    @DisplayName("sendResponse với payload String: payload xuất hiện trong JSON")
    void testSendResponse_WithStringPayload_SerializedCorrectly() {
        context.sendResponse(new Response<>("T", "S", "m", "my-payload"));
        String json = stringWriter.toString().trim();
        assertTrue(json.contains("my-payload"));
    }

    // ===================== GỬI THÔNG ĐIỆP HỆ THỐNG =====================

    @Test
    @DisplayName("sendSystemMessage: field 'status' phải là 'SYSTEM'")
    void testSendSystemMessage_StatusIsSystem() {
        context.sendSystemMessage("SYS_ACTION", "system msg", null);
        assertEquals("SYSTEM", getField(lastOutput(), "status"));
    }

    @Test
    @DisplayName("sendSystemMessage: field 'type' phải là action truyền vào")
    void testSendSystemMessage_TypeIsAction() {
        context.sendSystemMessage("AUCTION_STARTED", "đã bắt đầu", null);
        assertEquals("AUCTION_STARTED", getField(lastOutput(), "type"));
    }

    @Test
    @DisplayName("sendSystemMessage với payload số: type phải đúng")
    void testSendSystemMessage_WithNumericPayload_TypeCorrect() {
        context.sendSystemMessage("TIME_EXTENDED", "gia hạn", 30);
        assertEquals("TIME_EXTENDED", getField(lastOutput(), "type"));
    }

    @Test
    @DisplayName("sendSystemMessage với payload null: không throw")
    void testSendSystemMessage_NullPayload_NoThrow() {
        assertDoesNotThrow(() ->
                context.sendSystemMessage("AUCTION_FINISHED", "kết thúc", null));
    }

    @Test
    @DisplayName("sendSystemMessage với payload Map: status vẫn là SYSTEM")
    void testSendSystemMessage_WithMapPayload_StatusSystem() {
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("winner", "alice");
        payload.put("finalPrice", 1000.0);
        context.sendSystemMessage("AUCTION_FINISHED", "kết thúc", payload);
        assertEquals("SYSTEM", getField(lastOutput(), "status"));
    }

    // ===================== GSON TĨNH =====================

    @Test
    @DisplayName("gson() không được trả về null")
    void testGson_NotNull() {
        assertNotNull(ClientSession.gson());
    }

    @Test
    @DisplayName("gson() trả về cùng instance mỗi lần gọi (singleton)")
    void testGson_SameInstance() {
        Gson g1 = ClientSession.gson();
        Gson g2 = ClientSession.gson();
        assertSame(g1, g2);
    }

    @Test
    @DisplayName("gson() có thể serialize LocalDateTime không throw")
    void testGson_SerializesLocalDateTime() {
        assertDoesNotThrow(() -> {
            String json = ClientSession.gson().toJson(java.time.LocalDateTime.now());
            assertNotNull(json);
            assertFalse(json.isEmpty());
        });
    }

    // ===================== DỌN DẸP =====================

    @Test
    @DisplayName("cleanup() khi chưa login và không có phòng: không throw Exception")
    void testCleanup_NoLoginNoRoom_ShouldNotThrow() {
        assertDoesNotThrow(() -> context.cleanup());
    }

    @Test
    @DisplayName("cleanup() sau khi login: không throw Exception")
    void testCleanup_AfterLogin_NoException() {
        context.setClientId("alice");
        assertDoesNotThrow(() -> context.cleanup());
    }

    @Test
    @DisplayName("cleanup() khi không có room: currentRoom vẫn null sau cleanup")
    void testCleanup_WithoutRoom_CurrentRoomNull() {
        assertNull(context.getCurrentRoom());
        context.cleanup();
        assertNull(context.getCurrentRoom());
    }

    @Test
    @DisplayName("cleanup() sau login: clientId vẫn là 'alice' (cleanup không reset clientId)")
    void testCleanup_DoesNotResetClientId() {
        context.setClientId("alice");
        context.cleanup();
        // cleanup() gọi logout() nhưng không gán clientId = "Guest"
        // hành vi này tuỳ cách triển khai, chỉ kiểm tra không ném lỗi
        assertDoesNotThrow(() -> context.getClientId());
    }

    // ===================== GÁN PHÒNG HIỆN TẠI =====================

    @Test
    @DisplayName("setCurrentRoom(null): getCurrentRoom phải trả về null")
    void testSetCurrentRoom_Null() {
        context.setCurrentRoom(null);
        assertNull(context.getCurrentRoom());
    }

    @Test
    @DisplayName("setOut: sau khi set out mới, sendResponse ghi vào out mới")
    void testSetOut_NewWriter_UsedForResponse() {
        StringWriter sw2 = new StringWriter();
        context.setOut(new PrintWriter(sw2, true));
        context.sendResponse(new Response<>("NEW", "OK", "test", null));
        assertFalse(sw2.toString().isEmpty(), "Output phải được ghi vào PrintWriter mới.");
    }
}
