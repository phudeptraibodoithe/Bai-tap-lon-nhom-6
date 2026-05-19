import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test – User
 * User kế thừa Person (abstract), thêm description và avatarURL.
 */
public class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("user01", "Alice", "pass123", 5000.0, "Người mua tích cực", "avatar.png");
    }

    // ── Constructor / Fields ──────────────────────────────────
    @Test
    @DisplayName("Tạo User – accountName đúng")
    void testAccountName() {
        assertEquals("user01", user.getAccountName());
    }

    @Test
    @DisplayName("Tạo User – nickname đúng")
    void testNickname() {
        assertEquals("Alice", user.getNickname());
    }

    @Test
    @DisplayName("Tạo User – password đúng")
    void testPassword() {
        assertEquals("pass123", user.getPassword());
    }

    @Test
    @DisplayName("Tạo User – balance khởi tạo đúng")
    void testBalance() {
        assertEquals(5000.0, user.getBalance());
    }

    @Test
    @DisplayName("Tạo User – description đúng")
    void testDescription() {
        assertEquals("Người mua tích cực", user.getDescription());
    }

    @Test
    @DisplayName("Tạo User – avatarURL đúng")
    void testAvatarURL() {
        assertEquals("avatar.png", user.getAvatarURL());
    }

    // ── Kế thừa Person ───────────────────────────────────────
    @Test
    @DisplayName("User là instanceof Person")
    void testInstanceOfPerson() {
        assertInstanceOf(Person.class, user);
    }

    // ── setBalance ───────────────────────────────────────────
    @Test
    @DisplayName("setBalance – cập nhật số dư thành công")
    void testSetBalance() {
        user.setBalance(8000.0);
        assertEquals(8000.0, user.getBalance());
    }

    @Test
    @DisplayName("setBalance – cho phép balance = 0")
    void testSetBalanceZero() {
        user.setBalance(0.0);
        assertEquals(0.0, user.getBalance());
    }

    @Test
    @DisplayName("setBalance – cho phép balance âm (nợ)")
    void testSetBalanceNegative() {
        user.setBalance(-200.0);
        assertEquals(-200.0, user.getBalance());
    }

    // ── Edge cases ───────────────────────────────────────────
    @Test
    @DisplayName("Hai User khác nhau không bằng nhau theo accountName")
    void testDifferentUsers() {
        User user2 = new User("user02", "Bob", "pw456", 1000.0, "desc", "bob.png");
        assertNotEquals(user.getAccountName(), user2.getAccountName());
    }

    @Test
    @DisplayName("User có thể có balance = 0 khi tạo")
    void testCreateUserWithZeroBalance() {
        User poor = new User("user03", "Charlie", "pw", 0.0, "Mới đăng ký", "default.png");
        assertEquals(0.0, poor.getBalance());
    }
}
