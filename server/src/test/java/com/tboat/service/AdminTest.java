package com.tboat.service;

import org.junit.jupiter.api.*;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import com.tboat.models.*;

/**
 * Unit Test – Admin
 * Admin kế thừa Person (abstract), có thêm censorSession().
 */
public class AdminTest {

    private Admin           admin;
    private AuctionSession  session;
    private ElectronicsItem item;

    @BeforeEach
    void setUp() {
        admin   = new Admin("admin01", "AdminBob", "adminpass", 0.0);
        item    = new ElectronicsItem("seller01", "Laptop", "Mô tả", "img.png");
        session = new AuctionSession(101, item,
                LocalDateTime.now(), LocalDateTime.now().plusHours(2), 1000.0, 100.0);
    }

    // ── Constructor / Fields ──────────────────────────────────
    @Test
    @DisplayName("Tạo Admin – accountName đúng")
    void testAccountName() {
        assertEquals("admin01", admin.getAccountName());
    }

    @Test
    @DisplayName("Tạo Admin – nickname đúng")
    void testNickname() {
        assertEquals("AdminBob", admin.getNickname());
    }

    @Test
    @DisplayName("Tạo Admin – password đúng")
    void testPassword() {
        assertEquals("adminpass", admin.getPassword());
    }

    @Test
    @DisplayName("Tạo Admin – balance khởi tạo đúng")
    void testBalance() {
        assertEquals(0.0, admin.getBalance());
    }

    // ── Kế thừa Person ───────────────────────────────────────
    @Test
    @DisplayName("Admin là instanceof Person")
    void testInstanceOfPerson() {
        assertInstanceOf(Person.class, admin);
    }

    // ── censorSession ─────────────────────────────────────────
    @Test
    @DisplayName("censorSession – đổi trạng thái session thành CANCELED")
    void testCensorSessionCanceled() {
        session.setStatusOfAuction(StatusOfAuction.ONGOING);
        admin.censorSession(session);
        assertEquals(StatusOfAuction.CANCELED, session.getStatusOfAuction());
    }

    @Test
    @DisplayName("censorSession – có thể áp dụng lên session NOT_STARTED")
    void testCensorSessionNotStarted() {
        assertEquals(StatusOfAuction.NOT_STARTED, session.getStatusOfAuction());
        admin.censorSession(session);
        assertEquals(StatusOfAuction.CANCELED, session.getStatusOfAuction());
    }

    @Test
    @DisplayName("censorSession – có thể áp dụng lên session PENDING")
    void testCensorSessionPending() {
        session.setStatusOfAuction(StatusOfAuction.PENDING);
        admin.censorSession(session);
        assertEquals(StatusOfAuction.CANCELED, session.getStatusOfAuction());
    }

    @Test
    @DisplayName("censorSession – sau khi cancel, không thể đặt giá")
    void testBidFailedAfterCensor() {
        session.setStatusOfAuction(StatusOfAuction.ONGOING);
        admin.censorSession(session);
        boolean result = session.placeBid("user01", 2000.0);
        assertFalse(result);
    }

    @Test
    @DisplayName("censorSession – nhiều session khác nhau")
    void testCensorMultipleSessions() {
        FashionItem item2    = new FashionItem(2, "seller02", "Áo", "desc", "img2.png");
        AuctionSession sess2 = new AuctionSession(102, item2,
                LocalDateTime.now(), LocalDateTime.now().plusHours(1), 500.0, 50.0);

        admin.censorSession(session);
        admin.censorSession(sess2);

        assertAll(
            () -> assertEquals(StatusOfAuction.CANCELED, session.getStatusOfAuction()),
            () -> assertEquals(StatusOfAuction.CANCELED, sess2.getStatusOfAuction())
        );
    }

    // ── setBalance (từ Person) ────────────────────────────────
    @Test
    @DisplayName("Admin có thể cập nhật balance từ Person")
    void testSetBalance() {
        admin.setBalance(500.0);
        assertEquals(500.0, admin.getBalance());
    }
}
