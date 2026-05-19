import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test – Participation
 * Participation ghi nhận việc User tham gia AuctionSession với một vai trò (roleType).
 * Liên kết: User --joins--> AuctionSession | User --places--> AuctionSession | User --wins--> AuctionSession
 */
public class ParticipationTest {

    private Participation bidder;
    private Participation watcher;
    private Participation winner;

    @BeforeEach
    void setUp() {
        bidder  = new Participation("user01", 101, "BIDDER");
        watcher = new Participation("user02", 101, "WATCHER");
        winner  = new Participation("user03", 102, "WINNER");
    }

    // ── Constructor / Fields ──────────────────────────────────
    @Test
    @DisplayName("accountName đúng (BIDDER)")
    void testAccountNameBidder() {
        assertEquals("user01", bidder.getAccountName());
    }

    @Test
    @DisplayName("auctionSessionId đúng (BIDDER)")
    void testSessionIdBidder() {
        assertEquals(101, bidder.getAuctionSessionId());
    }

    @Test
    @DisplayName("roleType đúng (BIDDER)")
    void testRoleTypeBidder() {
        assertEquals("BIDDER", bidder.getRoleType());
    }

    @Test
    @DisplayName("accountName đúng (WATCHER)")
    void testAccountNameWatcher() {
        assertEquals("user02", watcher.getAccountName());
    }

    @Test
    @DisplayName("roleType đúng (WATCHER)")
    void testRoleTypeWatcher() {
        assertEquals("WATCHER", watcher.getRoleType());
    }

    @Test
    @DisplayName("roleType đúng (WINNER)")
    void testRoleTypeWinner() {
        assertEquals("WINNER", winner.getRoleType());
    }

    @Test
    @DisplayName("auctionSessionId đúng (WINNER – session khác)")
    void testSessionIdWinner() {
        assertEquals(102, winner.getAuctionSessionId());
    }

    // ── Nhiều participant cùng session ────────────────────────
    @Test
    @DisplayName("Hai participant khác nhau cùng session")
    void testTwoParticipantsSameSession() {
        assertAll(
            () -> assertEquals(bidder.getAuctionSessionId(), watcher.getAuctionSessionId()),
            () -> assertNotEquals(bidder.getAccountName(), watcher.getAccountName()),
            () -> assertNotEquals(bidder.getRoleType(),    watcher.getRoleType())
        );
    }

    @Test
    @DisplayName("Participant khác session có sessionId khác")
    void testDifferentSession() {
        assertNotEquals(bidder.getAuctionSessionId(), winner.getAuctionSessionId());
    }

    // ── roleType hợp lệ ───────────────────────────────────────
    @Test
    @DisplayName("Cùng một user có thể join hai session khác nhau")
    void testSameUserDifferentSessions() {
        Participation p1 = new Participation("user01", 101, "BIDDER");
        Participation p2 = new Participation("user01", 202, "WATCHER");
        assertAll(
            () -> assertEquals(p1.getAccountName(), p2.getAccountName()),
            () -> assertNotEquals(p1.getAuctionSessionId(), p2.getAuctionSessionId())
        );
    }

    @Test
    @DisplayName("Cùng một session có nhiều loại roleType")
    void testMultipleRolesPerSession() {
        Participation p1 = new Participation("user01", 101, "BIDDER");
        Participation p2 = new Participation("user02", 101, "WATCHER");
        Participation p3 = new Participation("user03", 101, "WINNER");
        assertAll(
            () -> assertEquals(101, p1.getAuctionSessionId()),
            () -> assertEquals(101, p2.getAuctionSessionId()),
            () -> assertEquals(101, p3.getAuctionSessionId()),
            () -> assertNotEquals(p1.getRoleType(), p2.getRoleType()),
            () -> assertNotEquals(p2.getRoleType(), p3.getRoleType())
        );
    }

    // ── Edge case ─────────────────────────────────────────────
    @Test
    @DisplayName("roleType có thể là chuỗi tùy ý")
    void testCustomRoleType() {
        Participation p = new Participation("user10", 999, "OBSERVER");
        assertEquals("OBSERVER", p.getRoleType());
    }
}
