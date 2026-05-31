package com.tboat.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.auction.AuctionSession;
import com.tboat.models.auction.BidResult;
import com.tboat.models.item.OtherItem;
import com.tboat.models.network.ServerEvent;
import com.tboat.socket.ClientSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuctionRoomAutoBidTest {

    private AuctionSessionDAO originalTimerSessionDAO;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        originalTimerSessionDAO = getTimerSessionDAO();
    }

    @AfterEach
    void tearDown() {
        injectTimerSessionDAO(originalTimerSessionDAO);
        AuctionManager.getInstance().removeRoom(777);
    }

    @Test
    @DisplayName("AuctionRoom: auto-bid thành công cập nhật giá và leader")
    void autoBidSuccessUpdatesPriceAndLeader() {
        AuctionRoom room = new AuctionRoom(777, 100);
        CapturingClient viewer = new CapturingClient("viewer");
        FakeAuctionSessionDAO sessionDAO = new FakeAuctionSessionDAO(sessionEndingInMinutes(10));
        injectRoomDependencies(room, sessionDAO, new FakeBiddingService(BidResult.OK), new FakeUserDAO());
        room.addSubscriber(viewer);

        room.registerAutoBid("alice", 130);

        assertEquals(110, room.getCurrentPrice());
        assertEquals("alice", room.getLastBidder());
        JsonObject response = viewer.waitForLastResponse();
        assertEquals(ServerEvent.NEW_BID.name(), response.get("type").getAsString());
        assertTrue(response.get("message").getAsString().contains("(auto)"));
    }

    @Test
    @DisplayName("AuctionRoom: auto-bid bị out khi giá kế tiếp vượt maxBid")
    void autoBidOutSendsPrivateNotification() {
        AuctionRoom room = new AuctionRoom(777, 100);
        CapturingClient alice = new CapturingClient("alice");
        FakeAuctionSessionDAO sessionDAO = new FakeAuctionSessionDAO(sessionEndingInMinutes(10));
        injectRoomDependencies(room, sessionDAO, new FakeBiddingService(BidResult.OK), new FakeUserDAO());
        room.addSubscriber(alice);

        room.registerAutoBid("alice", 105);

        assertEquals(100, room.getCurrentPrice());
        assertNull(room.getLastBidder());
        JsonObject response = alice.lastResponse();
        assertEquals(ServerEvent.AUTO_BID_OUT.name(), response.get("type").getAsString());
        assertEquals(-1.0, room.getAutoBidMax("alice"));
    }

    @Test
    @DisplayName("AuctionRoom: auto-bid bị out được gỡ khỏi room nên không báo lặp")
    void autoBidOutRemovesRegistration() {
        AuctionRoom room = new AuctionRoom(777, 100);
        CapturingClient alice = new CapturingClient("alice");
        FakeAuctionSessionDAO sessionDAO = new FakeAuctionSessionDAO(sessionEndingInMinutes(10));
        injectRoomDependencies(room, sessionDAO, new FakeBiddingService(BidResult.OK), new FakeUserDAO());
        room.addSubscriber(alice);

        room.registerAutoBid("alice", 105);
        room.triggerAutoBids();

        assertEquals(-1.0, room.getAutoBidMax("alice"));
        assertEquals(1, alice.responseCount(ServerEvent.AUTO_BID_OUT));
    }

    @Test
    @DisplayName("AuctionRoom: auto-bid vượt người vừa bid thì gửi notification outbid")
    void autoBidOutbidsManualBidderNotification() {
        AuctionRoom room = new AuctionRoom(777, 100);
        CapturingClient bob = new CapturingClient("bob");
        FakeAuctionSessionDAO sessionDAO = new FakeAuctionSessionDAO(sessionEndingInMinutes(10));
        injectRoomDependencies(room, sessionDAO, new FakeBiddingService(BidResult.OK), new FakeUserDAO());
        NotificationService.getInstance().register("bob", bob);

        try {
            room.registerAutoBid("alice", 150);
            assertEquals(BidResult.OK, room.placeBid(120, "bob"));
            room.triggerAutoBids();

            JsonObject response = bob.waitForNotification(ServerEvent.OUTBID);
            assertEquals(ServerEvent.NOTIFICATION.name(), response.get("type").getAsString());
            assertEquals(ServerEvent.OUTBID.name(),
                    response.getAsJsonObject("payload").get("notifType").getAsString());
        } finally {
            NotificationService.getInstance().unregister("bob");
        }
    }

    @Test
    @DisplayName("AuctionRoom: người có maxBid cao hơn tự động vượt giá người trước")
    void autoBidHigherMaxOutbidsPreviousLeader() {
        AuctionRoom room = new AuctionRoom(777, 100);
        CapturingClient viewer = new CapturingClient("viewer");
        FakeAuctionSessionDAO sessionDAO = new FakeAuctionSessionDAO(sessionEndingInMinutes(10));
        injectRoomDependencies(room, sessionDAO, new FakeBiddingService(BidResult.OK), new FakeUserDAO());
        room.addSubscriber(viewer);

        room.registerAutoBid("alice", 130);
        room.registerAutoBid("bob", 150);

        assertEquals(140, room.getCurrentPrice());
        assertEquals("bob", room.getLastBidder());
        JsonObject payload = viewer.waitForResponseWithLeader("bob").getAsJsonObject("payload");
        assertEquals("bob", payload.get("newLeader").getAsString());
        assertEquals(140, payload.get("newPrice").getAsDouble());
    }

    @Test
    @DisplayName("AuctionRoom: bid sát giờ chót kích hoạt gia hạn thời gian")
    void normalBidNearEndExtendsAuction() {
        AuctionRoom room = new AuctionRoom(777, 100);
        AuctionManager.getInstance().createRoom(777, 100);
        AuctionRoom managedRoom = AuctionManager.getInstance().getRoom(777);
        CapturingClient viewer = new CapturingClient("viewer");

        LocalDateTime endTime = LocalDateTime.now().plusSeconds(5);
        FakeAuctionSessionDAO roomSessionDAO = new FakeAuctionSessionDAO(sessionEndingAt(endTime));
        FakeAuctionSessionDAO timerSessionDAO = new FakeAuctionSessionDAO(sessionEndingAt(endTime));
        injectRoomDependencies(managedRoom, roomSessionDAO, new FakeBiddingService(BidResult.OK), new FakeUserDAO());
        injectTimerSessionDAO(timerSessionDAO);
        managedRoom.addSubscriber(viewer);

        BidResult result = managedRoom.placeBid(110, "alice");

        assertEquals(BidResult.OK, result);
        assertNotNull(timerSessionDAO.updatedEndTime);
        assertTrue(timerSessionDAO.updatedEndTime.isAfter(endTime));
        JsonObject response = viewer.waitForLastResponse();
        assertEquals(ServerEvent.TIME_EXTENDED.name(), response.get("type").getAsString());
        assertEquals(30, response.getAsJsonObject("payload").get("secondsAdded").getAsInt());
    }

    private static AuctionSession sessionEndingInMinutes(int minutes) {
        return sessionEndingAt(LocalDateTime.now().plusMinutes(minutes));
    }

    private static AuctionSession sessionEndingAt(LocalDateTime endTime) {
        AuctionSession session = new AuctionSession(
                LocalDateTime.now().minusMinutes(1),
                endTime,
                100,
                10,
                new OtherItem("seller", "Sản phẩm test", "Mô tả", "")
        );
        session.setId(777);
        session.setBuyNowPrice(0);
        return session;
    }

    private static void injectRoomDependencies(AuctionRoom room, AuctionSessionDAO sessionDAO,
                                               BiddingService biddingService, UserDAO userDAO) {
        setField(room, "sessionDAO", sessionDAO);
        setField(room, "biddingService", biddingService);
        setField(room, "userDAO", userDAO);
    }

    private static void injectTimerSessionDAO(AuctionSessionDAO sessionDAO) {
        setField(AuctionTimerService.getInstance(), "sessionDao", sessionDAO);
    }

    private static AuctionSessionDAO getTimerSessionDAO() {
        try {
            Field field = AuctionTimerService.getInstance().getClass().getDeclaredField("sessionDao");
            field.setAccessible(true);
            return (AuctionSessionDAO) field.get(AuctionTimerService.getInstance());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Không đọc được sessionDao của AuctionTimerService", e);
        }
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Không inject được field " + fieldName, e);
        }
    }

    private static class FakeAuctionSessionDAO extends AuctionSessionDAO {
        private final AuctionSession session;
        private LocalDateTime updatedEndTime;

        FakeAuctionSessionDAO(AuctionSession session) {
            this.session = session;
        }

        @Override
        public AuctionSession getAuctionById(int sessionId) {
            return session;
        }

        @Override
        public void updateEndTime(int sessionId, LocalDateTime newEndTime) {
            updatedEndTime = newEndTime;
            session.setEndTime(newEndTime);
        }
    }

    private static class FakeBiddingService extends BiddingService {
        private final BidResult result;

        FakeBiddingService(BidResult result) {
            this.result = result;
        }

        @Override
        public BidResult placeBid(String bidderAccount, int sessionId, double newPrice) {
            return result;
        }
    }

    private static class FakeUserDAO extends UserDAO {
        @Override
        public String getNickname(String accountName) {
            return accountName;
        }
    }

    private static class CapturingClient extends ClientSession {
        private final StringWriter buffer = new StringWriter();

        CapturingClient(String account) {
            setClientId(account);
            setOut(new PrintWriter(buffer, true));
        }

        JsonObject waitForLastResponse() {
            long deadline = System.currentTimeMillis() + 1_000;
            while (System.currentTimeMillis() < deadline) {
                if (!buffer.toString().trim().isBlank()) {
                    return lastResponse();
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    fail("Bị ngắt khi chờ broadcast");
                }
            }
            fail("Không nhận được broadcast");
            return null;
        }

        JsonObject waitForResponseWithLeader(String leader) {
            long deadline = System.currentTimeMillis() + 1_000;
            while (System.currentTimeMillis() < deadline) {
                String text = buffer.toString().trim();
                if (!text.isBlank()) {
                    String[] lines = text.split("\\R");
                    for (int i = lines.length - 1; i >= 0; i--) {
                        JsonObject response = JsonParser.parseString(lines[i]).getAsJsonObject();
                        if (response.has("payload")
                                && response.get("payload").isJsonObject()
                                && response.getAsJsonObject("payload").has("newLeader")
                                && leader.equals(response.getAsJsonObject("payload")
                                .get("newLeader").getAsString())) {
                            return response;
                        }
                    }
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    fail("Bị ngắt khi chờ broadcast");
                }
            }
            fail("Không nhận được broadcast có leader " + leader);
            return null;
        }

        JsonObject lastResponse() {
            String[] lines = buffer.toString().trim().split("\\R");
            assertTrue(lines.length > 0 && !lines[0].isBlank(), "Phải có response được gửi");
            return JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();
        }

        int responseCount(ServerEvent event) {
            String text = buffer.toString().trim();
            if (text.isBlank()) return 0;
            int count = 0;
            for (String line : text.split("\\R")) {
                JsonObject response = JsonParser.parseString(line).getAsJsonObject();
                if (event.name().equals(response.get("type").getAsString())) count++;
            }
            return count;
        }

        JsonObject waitForNotification(ServerEvent event) {
            long deadline = System.currentTimeMillis() + 1_000;
            while (System.currentTimeMillis() < deadline) {
                String text = buffer.toString().trim();
                if (!text.isBlank()) {
                    String[] lines = text.split("\\R");
                    for (int i = lines.length - 1; i >= 0; i--) {
                        JsonObject response = JsonParser.parseString(lines[i]).getAsJsonObject();
                        if (ServerEvent.NOTIFICATION.name().equals(response.get("type").getAsString())
                                && response.has("payload")
                                && response.get("payload").isJsonObject()
                                && event.name().equals(response.getAsJsonObject("payload")
                                .get("notifType").getAsString())) {
                            return response;
                        }
                    }
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    fail("Bị ngắt khi chờ notification");
                }
            }
            fail("Không nhận được notification " + event);
            return null;
        }
    }
}
