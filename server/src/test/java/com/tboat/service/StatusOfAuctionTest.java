import org.junit.jupiter.api.*;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test – StatusOfAuction (enum)
 * 5 giá trị theo diagram: NOT_STARTED, ONGOING, ENDED, PENDING, CANCELED
 */
public class StatusOfAuctionTest {

    private AuctionSession session;

    @BeforeEach
    void setUp() {
        ElectronicsItem item = new ElectronicsItem(1, "seller01", "Laptop", "desc", "img.png");
        session = new AuctionSession(101, item,
                LocalDateTime.now(), LocalDateTime.now().plusHours(2), 1000.0, 100.0);
    }

    // ── Tồn tại đủ 5 giá trị ─────────────────────────────────
    @Test
    @DisplayName("Enum có đúng 5 giá trị")
    void testEnumCount() {
        assertEquals(5, StatusOfAuction.values().length);
    }

    @Test
    @DisplayName("NOT_STARTED tồn tại")
    void testNotStarted() {
        assertNotNull(StatusOfAuction.NOT_STARTED);
    }

    @Test
    @DisplayName("ONGOING tồn tại")
    void testOngoing() {
        assertNotNull(StatusOfAuction.ONGOING);
    }

    @Test
    @DisplayName("ENDED tồn tại")
    void testEnded() {
        assertNotNull(StatusOfAuction.ENDED);
    }

    @Test
    @DisplayName("PENDING tồn tại")
    void testPending() {
        assertNotNull(StatusOfAuction.PENDING);
    }

    @Test
    @DisplayName("CANCELED tồn tại")
    void testCanceled() {
        assertNotNull(StatusOfAuction.CANCELED);
    }

    // ── valueOf ───────────────────────────────────────────────
    @Test
    @DisplayName("valueOf NOT_STARTED trả về đúng")
    void testValueOfNotStarted() {
        assertEquals(StatusOfAuction.NOT_STARTED, StatusOfAuction.valueOf("NOT_STARTED"));
    }

    @Test
    @DisplayName("valueOf ONGOING trả về đúng")
    void testValueOfOngoing() {
        assertEquals(StatusOfAuction.ONGOING, StatusOfAuction.valueOf("ONGOING"));
    }

    @Test
    @DisplayName("valueOf ENDED trả về đúng")
    void testValueOfEnded() {
        assertEquals(StatusOfAuction.ENDED, StatusOfAuction.valueOf("ENDED"));
    }

    @Test
    @DisplayName("valueOf PENDING trả về đúng")
    void testValueOfPending() {
        assertEquals(StatusOfAuction.PENDING, StatusOfAuction.valueOf("PENDING"));
    }

    @Test
    @DisplayName("valueOf CANCELED trả về đúng")
    void testValueOfCanceled() {
        assertEquals(StatusOfAuction.CANCELED, StatusOfAuction.valueOf("CANCELED"));
    }

    @Test
    @DisplayName("valueOf tên không tồn tại ném IllegalArgumentException")
    void testValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
            StatusOfAuction.valueOf("UNKNOWN"));
    }

    // ── Ứng dụng trên AuctionSession ─────────────────────────
    @Test
    @DisplayName("Session mặc định là NOT_STARTED")
    void testSessionDefaultStatus() {
        assertEquals(StatusOfAuction.NOT_STARTED, session.getStatusOfAuction());
    }

    @Test
    @DisplayName("Session chuyển sang ONGOING – bid được phép")
    void testOngoingAllowsBid() {
        session.setStatusOfAuction(StatusOfAuction.ONGOING);
        assertTrue(session.placeBid("user01", 1200.0));
    }

    @Test
    @DisplayName("Session ở ENDED – bid bị từ chối")
    void testEndedBlocksBid() {
        session.setStatusOfAuction(StatusOfAuction.ENDED);
        assertFalse(session.placeBid("user01", 2000.0));
    }

    @Test
    @DisplayName("Session ở PENDING – bid bị từ chối")
    void testPendingBlocksBid() {
        session.setStatusOfAuction(StatusOfAuction.PENDING);
        assertFalse(session.placeBid("user01", 2000.0));
    }

    @Test
    @DisplayName("Session ở CANCELED – bid bị từ chối")
    void testCanceledBlocksBid() {
        session.setStatusOfAuction(StatusOfAuction.CANCELED);
        assertFalse(session.placeBid("user01", 2000.0));
    }

    @Test
    @DisplayName("Session ở NOT_STARTED – bid bị từ chối")
    void testNotStartedBlocksBid() {
        // status mặc định là NOT_STARTED
        assertFalse(session.placeBid("user01", 2000.0));
    }

    // ── So sánh các giá trị ───────────────────────────────────
    @Test
    @DisplayName("Tất cả 5 giá trị đều khác nhau")
    void testAllDistinct() {
        StatusOfAuction[] all = StatusOfAuction.values();
        for (int i = 0; i < all.length; i++) {
            for (int j = i + 1; j < all.length; j++) {
                assertNotEquals(all[i], all[j]);
            }
        }
    }
}
