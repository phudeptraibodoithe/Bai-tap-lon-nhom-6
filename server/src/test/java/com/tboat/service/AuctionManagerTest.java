package com.tboat.service;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho AuctionManager (sau khi đã xóa autoStartAuctions).
 * KHÔNG cần DB — AuctionManager chỉ quản lý Map in-memory.
 */
class AuctionManagerTest {

    private AuctionManager manager;

    // ID dùng riêng cho test, tránh xung đột với data thật
    private static final int ID_A = 9001;
    private static final int ID_B = 9002;
    private static final int ID_C = 9003;

    @BeforeEach
    void setUp() {
        manager = AuctionManager.getInstance();
    }

    @AfterEach
    void tearDown() {
        manager.removeRoom(ID_A);
        manager.removeRoom(ID_B);
        manager.removeRoom(ID_C);
    }

    // ===================== SINGLETON =====================

    @Test
    @DisplayName("getInstance() phải trả về cùng một object (Singleton)")
    void testSingleton_SameInstance() {
        AuctionManager i1 = AuctionManager.getInstance();
        AuctionManager i2 = AuctionManager.getInstance();
        assertSame(i1, i2, "AuctionManager phải là Singleton.");
    }

    @Test
    @DisplayName("getInstance() không được trả về null")
    void testSingleton_NotNull() {
        assertNotNull(AuctionManager.getInstance());
    }

    // ===================== CREATE ROOM =====================

    @Test
    @DisplayName("createRoom rồi getRoom: phải trả về room đúng sessionId")
    void testCreateAndGet_SessionIdMatches() {
        manager.createRoom(ID_A, 50_000.0);
        AuctionRoom room = manager.getRoom(ID_A);

        assertNotNull(room);
        assertEquals(ID_A, room.getSessionId());
    }

    @Test
    @DisplayName("createRoom rồi getRoom: giá khởi điểm phải khớp")
    void testCreateAndGet_InitialPriceMatches() {
        manager.createRoom(ID_A, 75_000.0);
        assertEquals(75_000.0, manager.getRoom(ID_A).getCurrentPrice());
    }

    @Test
    @DisplayName("createRoom: phòng mới phải chưa có lastBidder")
    void testCreateRoom_NoBidderInitially() {
        manager.createRoom(ID_A, 10_000.0);
        assertNull(manager.getRoom(ID_A).getLastBidder());
    }

    @Test
    @DisplayName("createRoom: phòng mới chưa ở trạng thái finished")
    void testCreateRoom_NotFinishedInitially() {
        manager.createRoom(ID_A, 10_000.0);
        assertFalse(manager.getRoom(ID_A).isFinished());
    }

    @Test
    @DisplayName("createRoom với giá = 0: được tạo bình thường")
    void testCreateRoom_ZeroPrice_Allowed() {
        manager.createRoom(ID_A, 0.0);
        assertNotNull(manager.getRoom(ID_A));
        assertEquals(0.0, manager.getRoom(ID_A).getCurrentPrice());
    }

    @Test
    @DisplayName("createRoom với giá âm: vẫn tạo được (không validate giá ở tầng này)")
    void testCreateRoom_NegativePrice_Allowed() {
        manager.createRoom(ID_A, -500.0);
        assertNotNull(manager.getRoom(ID_A));
    }

    // ===================== putIfAbsent — KHÔNG GHI ĐÈ =====================

    @Test
    @DisplayName("createRoom trùng ID: phòng đầu tiên phải được giữ nguyên")
    void testCreateRoom_Duplicate_KeepsOriginal() {
        manager.createRoom(ID_A, 100_000.0);
        manager.createRoom(ID_A, 999_999.0); // gọi lại cùng ID

        assertEquals(100_000.0, manager.getRoom(ID_A).getCurrentPrice(),
                "putIfAbsent: giá gốc phải được giữ, không bị ghi đè.");
    }

    @Test
    @DisplayName("createRoom trùng ID: vẫn chỉ có đúng 1 phòng cho ID đó")
    void testCreateRoom_Duplicate_OneRoomOnly() {
        manager.createRoom(ID_A, 100_000.0);
        AuctionRoom first = manager.getRoom(ID_A);

        manager.createRoom(ID_A, 200_000.0);
        AuctionRoom second = manager.getRoom(ID_A);

        assertSame(first, second, "Phải là cùng 1 object AuctionRoom.");
    }

    // ===================== GET ROOM =====================

    @Test
    @DisplayName("getRoom với ID không tồn tại → null")
    void testGetRoom_NotExist_ReturnsNull() {
        assertNull(manager.getRoom(99_999));
    }

    @Test
    @DisplayName("getRoom với ID âm không tồn tại → null")
    void testGetRoom_NegativeId_ReturnsNull() {
        assertNull(manager.getRoom(-1));
    }

    // ===================== REMOVE ROOM =====================

    @Test
    @DisplayName("removeRoom sau createRoom: getRoom phải trả về null")
    void testRemoveRoom_AfterCreate_ReturnsNull() {
        manager.createRoom(ID_A, 50_000.0);
        assertNotNull(manager.getRoom(ID_A));

        manager.removeRoom(ID_A);
        assertNull(manager.getRoom(ID_A));
    }

    @Test
    @DisplayName("removeRoom ID không tồn tại: không throw Exception")
    void testRemoveRoom_NotExist_NoThrow() {
        assertDoesNotThrow(() -> manager.removeRoom(99_999));
    }

    @Test
    @DisplayName("removeRoom ID âm: không throw Exception")
    void testRemoveRoom_NegativeId_NoThrow() {
        assertDoesNotThrow(() -> manager.removeRoom(-1));
    }

    @Test
    @DisplayName("removeRoom gọi 2 lần cùng ID: không throw Exception")
    void testRemoveRoom_CalledTwice_NoThrow() {
        manager.createRoom(ID_A, 10_000.0);
        manager.removeRoom(ID_A);
        assertDoesNotThrow(() -> manager.removeRoom(ID_A));
    }

    // ===================== NHIỀU PHÒNG CÙNG LÚC =====================

    @Test
    @DisplayName("Tạo 3 phòng khác nhau: tất cả đều tồn tại độc lập")
    void testMultipleRooms_AllExistIndependently() {
        manager.createRoom(ID_A, 1_000.0);
        manager.createRoom(ID_B, 2_000.0);
        manager.createRoom(ID_C, 3_000.0);

        assertNotNull(manager.getRoom(ID_A));
        assertNotNull(manager.getRoom(ID_B));
        assertNotNull(manager.getRoom(ID_C));
        assertNotSame(manager.getRoom(ID_A), manager.getRoom(ID_B));
        assertNotSame(manager.getRoom(ID_B), manager.getRoom(ID_C));
    }

    @Test
    @DisplayName("Xóa 1 trong 3 phòng: 2 phòng còn lại không bị ảnh hưởng")
    void testRemoveRoom_OneOfThree_OthersIntact() {
        manager.createRoom(ID_A, 1_000.0);
        manager.createRoom(ID_B, 2_000.0);
        manager.createRoom(ID_C, 3_000.0);

        manager.removeRoom(ID_B);

        assertNotNull(manager.getRoom(ID_A), "Phòng A phải còn nguyên.");
        assertNull(manager.getRoom(ID_B),    "Phòng B phải đã bị xóa.");
        assertNotNull(manager.getRoom(ID_C), "Phòng C phải còn nguyên.");
    }

    @Test
    @DisplayName("Tạo lại phòng sau khi đã xóa: tạo thành công với giá mới")
    void testCreateRoom_AfterRemove_CreatesNew() {
        manager.createRoom(ID_A, 1_000.0);
        manager.removeRoom(ID_A);

        manager.createRoom(ID_A, 5_000.0);
        AuctionRoom newRoom = manager.getRoom(ID_A);

        assertNotNull(newRoom);
        assertEquals(5_000.0, newRoom.getCurrentPrice(),
                "Phòng mới sau khi xóa phải có giá mới.");
    }
}