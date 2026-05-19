import org.junit.jupiter.api.*;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test – History
 * History lưu kết quả sau khi AuctionSession kết thúc:
 *   auctionSessionId, winnerAccount, finalPrice, completedAt.
 */
public class HistoryTest {

    private LocalDateTime now;
    private History       history;

    @BeforeEach
    void setUp() {
        now     = LocalDateTime.now();
        history = new History(101, "user01", 2500.0, now);
    }

    // ── Constructor / Fields ──────────────────────────────────
    @Test
    @DisplayName("auctionSessionId đúng")
    void testAuctionSessionId() {
        assertEquals(101, history.getAuctionSessionId());
    }

    @Test
    @DisplayName("winnerAccount đúng")
    void testWinnerAccount() {
        assertEquals("user01", history.getWinnerAccount());
    }

    @Test
    @DisplayName("finalPrice đúng")
    void testFinalPrice() {
        assertEquals(2500.0, history.getFinalPrice());
    }

    @Test
    @DisplayName("completedAt đúng")
    void testCompletedAt() {
        assertEquals(now, history.getCompletedAt());
    }

    // ── Liên kết với AuctionSession ───────────────────────────
    @Test
    @DisplayName("History liên kết đúng auctionSessionId với session")
    void testLinkedToSession() {
        ElectronicsItem item    = new ElectronicsItem(1, "seller01", "Laptop", "desc", "img.png");
        AuctionSession  session = new AuctionSession(101, item,
                now.minusHours(2), now, 1000.0, 100.0);

        assertEquals(session.getId(), history.getAuctionSessionId());
    }

    @Test
    @DisplayName("finalPrice khớp với giá thắng cuối cùng trong session")
    void testFinalPriceMatchesSession() {
        ElectronicsItem item    = new ElectronicsItem(1, "seller01", "Laptop", "desc", "img.png");
        AuctionSession  session = new AuctionSession(101, item,
                now.minusHours(2), now, 1000.0, 100.0);
        session.setStatusOfAuction(StatusOfAuction.ONGOING);
        session.placeBid("user01", 2500.0);
        session.setStatusOfAuction(StatusOfAuction.ENDED);

        History h = new History(session.getId(),
                                session.getHighestBidderAccount(),
                                session.getCurrentPrice(), now);
        assertEquals(session.getCurrentPrice(), h.getFinalPrice());
        assertEquals(session.getHighestBidderAccount(), h.getWinnerAccount());
    }

    // ── Nhiều History ─────────────────────────────────────────
    @Test
    @DisplayName("Hai History từ hai session khác nhau có sessionId khác nhau")
    void testDifferentSessions() {
        History h2 = new History(202, "user02", 800.0, now.plusDays(1));
        assertNotEquals(history.getAuctionSessionId(), h2.getAuctionSessionId());
    }

    @Test
    @DisplayName("completedAt của history sau muộn hơn history trước")
    void testCompletedAtOrder() {
        History h2 = new History(202, "user02", 800.0, now.plusHours(5));
        assertTrue(h2.getCompletedAt().isAfter(history.getCompletedAt()));
    }

    // ── Edge cases ────────────────────────────────────────────
    @Test
    @DisplayName("winnerAccount có thể là null nếu không ai đấu giá")
    void testNullWinner() {
        History noWinner = new History(103, null, 0.0, now);
        assertNull(noWinner.getWinnerAccount());
    }

    @Test
    @DisplayName("finalPrice = 0 khi session kết thúc không có bid")
    void testZeroFinalPrice() {
        History noWinner = new History(104, null, 0.0, now);
        assertEquals(0.0, noWinner.getFinalPrice());
    }
}
