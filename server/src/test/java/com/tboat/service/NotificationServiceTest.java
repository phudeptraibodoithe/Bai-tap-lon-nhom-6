package com.tboat.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.models.auction.AuctionSession;
import com.tboat.models.item.OtherItem;
import com.tboat.models.network.ServerEvent;
import com.tboat.socket.ClientSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificationServiceTest {

    private final NotificationService service = NotificationService.getInstance();

    @AfterEach
    void tearDown() {
        List.of("seller", "bidder", "winner", "user").forEach(service::unregister);
    }

    @Test
    @DisplayName("NotificationService: thông báo duyệt sản phẩm đúng type và nội dung")
    void itemApprovedNotification() {
        CapturingClient client = register("seller");

        service.onItemApproved("seller", "Đồng hồ");

        JsonObject payload = client.lastPayload();
        assertNotificationEnvelope(client.lastResponse());
        assertEquals(ServerEvent.ITEM_APPROVED.name(), payload.get("notifType").getAsString());
        assertEquals("Sản phẩm được duyệt thành công!", payload.get("title").getAsString());
        assertTrue(payload.get("subtitle").getAsString().contains("Đồng hồ"));
    }

    @Test
    @DisplayName("NotificationService: thông báo từ chối sản phẩm dùng icon text ổn định")
    void itemRejectedNotification() {
        CapturingClient client = register("seller");

        service.onItemRejected("seller", "Điện thoại", "Ảnh không hợp lệ");

        JsonObject payload = client.lastPayload();
        assertEquals(ServerEvent.ITEM_REJECTED.name(), payload.get("notifType").getAsString());
        assertEquals("!", payload.get("avatarText").getAsString());
        assertTrue(payload.get("subtitle").getAsString().contains("Ảnh không hợp lệ"));
    }

    @Test
    @DisplayName("NotificationService: thông báo bid mới gửi về seller")
    void newBidNotification() {
        CapturingClient seller = register("seller");

        service.onNewBid(session("seller"), "Nguyễn Văn A", 1_500_000);

        JsonObject payload = seller.lastPayload();
        assertEquals(ServerEvent.NEW_BID.name(), payload.get("notifType").getAsString());
        assertTrue(payload.get("title").getAsString().contains("Nguyễn Văn A"));
        assertEquals("NA", payload.get("avatarText").getAsString());
    }

    @Test
    @DisplayName("NotificationService: thông báo bị vượt giá gửi đúng bidder")
    void outbidNotification() {
        CapturingClient bidder = register("bidder");

        service.onOutbid(session("seller"), "bidder", 2_000_000);

        JsonObject payload = bidder.lastPayload();
        assertEquals(ServerEvent.OUTBID.name(), payload.get("notifType").getAsString());
        assertEquals("Bạn vừa bị vượt giá!", payload.get("title").getAsString());
        assertTrue(payload.get("subtitle").getAsString().contains("2.000.000 ₫"));
    }

    @Test
    @DisplayName("NotificationService: thông báo thắng phiên gửi về winner")
    void auctionWonNotification() {
        CapturingClient winner = register("winner");

        service.onAuctionWon(session("seller"), "winner", 3_000_000);

        JsonObject payload = winner.lastPayload();
        assertEquals(ServerEvent.AUCTION_WON.name(), payload.get("notifType").getAsString());
        assertTrue(payload.get("subtitle").getAsString().contains("3.000.000 ₫"));
    }

    @Test
    @DisplayName("NotificationService: thông báo đã bán gửi về seller")
    void auctionSoldNotification() {
        CapturingClient seller = register("seller");

        service.onAuctionSold(session("seller"), "winner", 3_000_000);

        JsonObject payload = seller.lastPayload();
        assertEquals(ServerEvent.AUCTION_SOLD.name(), payload.get("notifType").getAsString());
        assertTrue(payload.get("subtitle").getAsString().contains("winner"));
    }

    @Test
    @DisplayName("NotificationService: thông báo nạp tiền")
    void depositNotification() {
        CapturingClient user = register("user");

        service.onDeposit("user", 100_000, 500_000);

        JsonObject payload = user.lastPayload();
        assertEquals(ServerEvent.DEPOSIT.name(), payload.get("notifType").getAsString());
        assertTrue(payload.get("subtitle").getAsString().contains("+100.000 ₫"));
        assertTrue(payload.get("subtitle").getAsString().contains("500.000 ₫"));
    }

    @Test
    @DisplayName("NotificationService: thông báo rút tiền")
    void withdrawNotification() {
        CapturingClient user = register("user");

        service.onWithdraw("user", 50_000, 450_000);

        JsonObject payload = user.lastPayload();
        assertEquals(ServerEvent.WITHDRAW.name(), payload.get("notifType").getAsString());
        assertTrue(payload.get("subtitle").getAsString().contains("-50.000 ₫"));
        assertTrue(payload.get("subtitle").getAsString().contains("450.000 ₫"));
    }

    @Test
    @DisplayName("NotificationService: user offline không làm ném lỗi")
    void offlineUserDoesNotThrow() {
        assertDoesNotThrow(() -> service.onDeposit("offline-user", 1_000, 1_000));
    }

    private CapturingClient register(String username) {
        CapturingClient client = new CapturingClient(username);
        service.register(username, client);
        return client;
    }

    private static AuctionSession session(String seller) {
        AuctionSession session = new AuctionSession(
                LocalDateTime.now().plusMinutes(1),
                LocalDateTime.now().plusHours(1),
                100,
                10,
                new OtherItem(seller, "Sản phẩm test", "Mô tả", "")
        );
        session.setId(9);
        return session;
    }

    private static void assertNotificationEnvelope(JsonObject response) {
        assertEquals(ServerEvent.NOTIFICATION.name(), response.get("type").getAsString());
        assertEquals(ServerEvent.SYSTEM.name(), response.get("status").getAsString());
    }

    private static class CapturingClient extends ClientSession {
        private final StringWriter buffer = new StringWriter();

        CapturingClient(String account) {
            setClientId(account);
            setOut(new PrintWriter(buffer, true));
        }

        JsonObject lastResponse() {
            String[] lines = buffer.toString().trim().split("\\R");
            assertTrue(lines.length > 0 && !lines[0].isBlank(), "Phải có response được gửi");
            return JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();
        }

        JsonObject lastPayload() {
            return lastResponse().getAsJsonObject("payload");
        }
    }
}
