import org.junit.jupiter.api.*;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test – AuctionSession
 * AuctionSession là abstract class, has-a Item (composition).
 * Không còn subclass riêng theo loại item.
 */
public class AuctionSessionTest {

    private LocalDateTime   now;
    private ElectronicsItem electronicsItem;
    private FashionItem     fashionItem;
    private JewelryItem     jewelryItem;
    private OtherItem       otherItem;
    private AuctionSession  session;

    @BeforeEach
    void setUp() {
        now             = LocalDateTime.now();
        electronicsItem = new ElectronicsItem(1, "seller01", "Laptop", "Mô tả", "img.png");
        fashionItem     = new FashionItem    (2, "seller02", "Áo",     "Mô tả", "img2.png");
        jewelryItem     = new JewelryItem    (3, "seller03", "Nhẫn",   "Mô tả", "img3.png");
        otherItem       = new OtherItem      (4, "seller04", "Tranh",  "Mô tả", "img4.png");
        session         = new AuctionSession(101, electronicsItem, now, now.plusHours(2), 1000.0, 100.0);
    }

    // ── Constructor ───────────────────────────────────────────
    @Nested
    @DisplayName("Constructor & Khởi tạo")
    class ConstructorTests {

        @Test
        @DisplayName("id đúng")
        void testId() { assertEquals(101, session.getId()); }

        @Test
        @DisplayName("item được gán đúng (has-a)")
        void testItem() { assertSame(electronicsItem, session.getItem()); }

        @Test
        @DisplayName("startTime đúng")
        void testStartTime() { assertEquals(now, session.getStartTime()); }

        @Test
        @DisplayName("endTime đúng")
        void testEndTime() { assertEquals(now.plusHours(2), session.getEndTime()); }

        @Test
        @DisplayName("currentPrice khởi tạo đúng")
        void testCurrentPrice() { assertEquals(1000.0, session.getCurrentPrice()); }

        @Test
        @DisplayName("bidIncrease khởi tạo đúng")
        void testBidIncrease() { assertEquals(100.0, session.getBidIncrease()); }

        @Test
        @DisplayName("statusOfAuction mặc định là NOT_STARTED")
        void testDefaultStatus() { assertEquals(StatusOfAuction.NOT_STARTED, session.getStatusOfAuction()); }

        @Test
        @DisplayName("highestBidderAccount mặc định null")
        void testDefaultHighestBidder() { assertNull(session.getHighestBidderAccount()); }
    }

    // ── Validation ────────────────────────────────────────────
    @Nested
    @DisplayName("Validation khi tạo")
    class ValidationTests {

        @Test
        @DisplayName("Ném IllegalArgumentException khi item = null")
        void testNullItem() {
            assertThrows(IllegalArgumentException.class, () ->
                new AuctionSession(102, null, now, now.plusHours(1), 1000.0, 100.0));
        }

        @Test
        @DisplayName("Ném IllegalArgumentException khi bidIncrease = 0")
        void testZeroBidIncrease() {
            assertThrows(IllegalArgumentException.class, () ->
                new AuctionSession(103, electronicsItem, now, now.plusHours(1), 1000.0, 0.0));
        }

        @Test
        @DisplayName("Ném IllegalArgumentException khi bidIncrease âm")
        void testNegativeBidIncrease() {
            assertThrows(IllegalArgumentException.class, () ->
                new AuctionSession(104, electronicsItem, now, now.plusHours(1), 1000.0, -50.0));
        }
    }

    // ── Has-A Item (Composition) ──────────────────────────────
    @Nested
    @DisplayName("Has-A Item – Composition với 4 loại")
    class HasAItemTests {

        @Test
        @DisplayName("Session với ElectronicsItem – item đúng loại")
        void testElectronicsItem() {
            assertInstanceOf(ElectronicsItem.class, session.getItem());
            assertEquals("ELECTRONICS", session.getItem().getType());
        }

        @Test
        @DisplayName("Session với FashionItem – item đúng loại")
        void testFashionItem() {
            AuctionSession s = new AuctionSession(102, fashionItem, now, now.plusHours(1), 500.0, 50.0);
            assertInstanceOf(FashionItem.class, s.getItem());
            assertEquals("FASHION", s.getItem().getType());
        }

        @Test
        @DisplayName("Session với JewelryItem – item đúng loại")
        void testJewelryItem() {
            AuctionSession s = new AuctionSession(103, jewelryItem, now, now.plusHours(3), 3000.0, 200.0);
            assertInstanceOf(JewelryItem.class, s.getItem());
            assertEquals("JEWELRY", s.getItem().getType());
        }

        @Test
        @DisplayName("Session với OtherItem – item đúng loại")
        void testOtherItem() {
            AuctionSession s = new AuctionSession(104, otherItem, now, now.plusHours(1), 300.0, 30.0);
            assertInstanceOf(OtherItem.class, s.getItem());
            assertEquals("OTHER", s.getItem().getType());
        }

        @Test
        @DisplayName("Item trong session không null")
        void testItemNotNull() {
            assertNotNull(session.getItem());
        }

        @Test
        @DisplayName("Item trong session là instanceof Item")
        void testItemBaseClass() {
            assertInstanceOf(Item.class, session.getItem());
        }
    }

    // ── setStatusOfAuction ────────────────────────────────────
    @Nested
    @DisplayName("Chuyển trạng thái StatusOfAuction")
    class StatusTests {

        @Test
        @DisplayName("NOT_STARTED -> ONGOING")
        void testToOngoing() {
            session.setStatusOfAuction(StatusOfAuction.ONGOING);
            assertEquals(StatusOfAuction.ONGOING, session.getStatusOfAuction());
        }

        @Test
        @DisplayName("ONGOING -> ENDED")
        void testToEnded() {
            session.setStatusOfAuction(StatusOfAuction.ONGOING);
            session.setStatusOfAuction(StatusOfAuction.ENDED);
            assertEquals(StatusOfAuction.ENDED, session.getStatusOfAuction());
        }

        @Test
        @DisplayName("Bất kỳ trạng thái -> CANCELED")
        void testToCanceled() {
            session.setStatusOfAuction(StatusOfAuction.CANCELED);
            assertEquals(StatusOfAuction.CANCELED, session.getStatusOfAuction());
        }

        @Test
        @DisplayName("NOT_STARTED -> PENDING")
        void testToPending() {
            session.setStatusOfAuction(StatusOfAuction.PENDING);
            assertEquals(StatusOfAuction.PENDING, session.getStatusOfAuction());
        }
    }

    // ── placeBid ─────────────────────────────────────────────
    @Nested
    @DisplayName("placeBid – đặt giá")
    class PlaceBidTests {

        @BeforeEach
        void setOngoing() {
            session.setStatusOfAuction(StatusOfAuction.ONGOING);
        }

        @Test
        @DisplayName("Đặt giá hợp lệ – trả về true")
        void testBidSuccess() {
            assertTrue(session.placeBid("user01", 1200.0));
        }

        @Test
        @DisplayName("Đặt giá hợp lệ – currentPrice cập nhật")
        void testBidUpdatesCurrentPrice() {
            session.placeBid("user01", 1200.0);
            assertEquals(1200.0, session.getCurrentPrice());
        }

        @Test
        @DisplayName("Đặt giá hợp lệ – highestBidderAccount cập nhật")
        void testBidUpdatesHighestBidder() {
            session.placeBid("user01", 1200.0);
            assertEquals("user01", session.getHighestBidderAccount());
        }

        @Test
        @DisplayName("Đặt giá bằng đúng currentPrice + bidIncrease – hợp lệ")
        void testBidExactMinimum() {
            assertTrue(session.placeBid("user01", 1100.0)); // 1000 + 100
        }

        @Test
        @DisplayName("Đặt giá thấp hơn minimum – trả về false")
        void testBidTooLow() {
            assertFalse(session.placeBid("user01", 1050.0));
        }

        @Test
        @DisplayName("Đặt giá khi ENDED – trả về false")
        void testBidWhenEnded() {
            session.setStatusOfAuction(StatusOfAuction.ENDED);
            assertFalse(session.placeBid("user01", 2000.0));
        }

        @Test
        @DisplayName("Đặt giá khi NOT_STARTED – trả về false")
        void testBidWhenNotStarted() {
            session.setStatusOfAuction(StatusOfAuction.NOT_STARTED);
            assertFalse(session.placeBid("user01", 2000.0));
        }

        @Test
        @DisplayName("Đặt giá khi CANCELED – trả về false")
        void testBidWhenCanceled() {
            session.setStatusOfAuction(StatusOfAuction.CANCELED);
            assertFalse(session.placeBid("user01", 2000.0));
        }

        @Test
        @DisplayName("Nhiều người bid – người sau cao hơn thắng")
        void testMultipleBidders() {
            session.placeBid("user01", 1200.0);
            session.placeBid("user02", 1500.0);
            assertAll(
                () -> assertEquals("user02", session.getHighestBidderAccount()),
                () -> assertEquals(1500.0,   session.getCurrentPrice())
            );
        }

        @Test
        @DisplayName("Bid thấp hơn giá hiện tại không ảnh hưởng")
        void testLowerBidNoEffect() {
            session.placeBid("user01", 1500.0);
            session.placeBid("user02", 1200.0); // thấp hơn -> false
            assertEquals("user01", session.getHighestBidderAccount());
        }
    }
}
