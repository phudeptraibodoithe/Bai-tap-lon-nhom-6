import org.junit.jupiter.api.*;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test – Bid
 * Bid ghi nhận một lần đặt giá: auctionSessionId, bidderAccount, bidAmount, bidTime.
 */
public class BidTest {

    private LocalDateTime now;
    private Bid           bid;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        bid = new Bid(1, 101, "user01", 1200.0, now);
    }

    // ── Constructor / Fields ──────────────────────────────────
    @Test
    @DisplayName("id đúng")
    void testId() {
        assertEquals(1, bid.getId());
    }

    @Test
    @DisplayName("auctionSessionId đúng")
    void testAuctionSessionId() {
        assertEquals(101, bid.getAuctionSessionId());
    }

    @Test
    @DisplayName("bidderAccount đúng")
    void testBidderAccount() {
        assertEquals("user01", bid.getBidderAccount());
    }

    @Test
    @DisplayName("bidAmount đúng")
    void testBidAmount() {
        assertEquals(1200.0, bid.getBidAmount());
    }

    @Test
    @DisplayName("bidTime đúng")
    void testBidTime() {
        assertEquals(now, bid.getBidTime());
    }

    // ── Validation ────────────────────────────────────────────
    @Test
    @DisplayName("Ném IllegalArgumentException khi bidAmount = 0")
    void testZeroBidAmount() {
        assertThrows(IllegalArgumentException.class, () ->
            new Bid(2, 101, "user01", 0.0, now));
    }

    @Test
    @DisplayName("Ném IllegalArgumentException khi bidAmount âm")
    void testNegativeBidAmount() {
        assertThrows(IllegalArgumentException.class, () ->
            new Bid(3, 101, "user01", -500.0, now));
    }

    // ── Thứ tự và so sánh ────────────────────────────────────
    @Test
    @DisplayName("Bid sau có bidAmount cao hơn bid trước")
    void testBidOrder() {
        Bid bid2 = new Bid(2, 101, "user02", 1500.0, now.plusMinutes(5));
        assertTrue(bid2.getBidAmount() > bid.getBidAmount());
    }

    @Test
    @DisplayName("Bid sau có bidTime muộn hơn bid trước")
    void testBidTimeOrder() {
        Bid bid2 = new Bid(2, 101, "user02", 1500.0, now.plusMinutes(5));
        assertTrue(bid2.getBidTime().isAfter(bid.getBidTime()));
    }

    @Test
    @DisplayName("Hai bid cùng session có auctionSessionId giống nhau")
    void testSameSessionId() {
        Bid bid2 = new Bid(2, 101, "user02", 1500.0, now.plusMinutes(3));
        assertEquals(bid.getAuctionSessionId(), bid2.getAuctionSessionId());
    }

    @Test
    @DisplayName("Hai bid khác session có auctionSessionId khác nhau")
    void testDifferentSessionId() {
        Bid bid2 = new Bid(3, 202, "user02", 900.0, now.plusMinutes(1));
        assertNotEquals(bid.getAuctionSessionId(), bid2.getAuctionSessionId());
    }

    @Test
    @DisplayName("Hai bid khác người dùng")
    void testDifferentBidders() {
        Bid bid2 = new Bid(4, 101, "user02", 1300.0, now.plusMinutes(2));
        assertNotEquals(bid.getBidderAccount(), bid2.getBidderAccount());
    }

    @Test
    @DisplayName("Bid có id duy nhất")
    void testUniqueId() {
        Bid bid2 = new Bid(2, 101, "user01", 1300.0, now.plusMinutes(1));
        assertNotEquals(bid.getId(), bid2.getId());
    }

    // ── Edge case: giá trị hợp lệ nhỏ nhất ──────────────────
    @Test
    @DisplayName("bidAmount = 0.01 là hợp lệ (giá trị dương nhỏ nhất)")
    void testMinimalPositiveBidAmount() {
        Bid smallBid = new Bid(10, 101, "user01", 0.01, now);
        assertEquals(0.01, smallBid.getBidAmount(), 1e-9);
    }
}
