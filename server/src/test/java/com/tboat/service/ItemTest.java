import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test – Item (abstract) và 4 subclass:
 *   ElectronicsItem | FashionItem | JewelryItem | OtherItem
 */
public class ItemTest {

    // ── Shared fixtures ───────────────────────────────────────
    private ElectronicsItem electronicsItem;
    private FashionItem     fashionItem;
    private JewelryItem     jewelryItem;
    private OtherItem       otherItem;

    @BeforeEach
    void setUp() {
        electronicsItem = new ElectronicsItem(1, "seller01", "Laptop Gaming", "RTX 4080", "laptop.png");
        fashionItem     = new FashionItem    (2, "seller02", "Áo Dài Cưới",   "Lụa thêu", "aodai.png");
        jewelryItem     = new JewelryItem    (3, "seller03", "Nhẫn Kim Cương","2 carat",  "ring.png");
        otherItem       = new OtherItem      (4, "seller04", "Tranh Sơn Dầu", "Nghệ thuật","painting.png");
    }

    // ══════════════════════════════════════════════════════════
    //  ElectronicsItem
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("ElectronicsItem")
    class ElectronicsItemTests {

        @Test
        @DisplayName("id đúng")
        void testId() { assertEquals(1, electronicsItem.getId()); }

        @Test
        @DisplayName("sellerAccountName đúng")
        void testSeller() { assertEquals("seller01", electronicsItem.getSellerAccountName()); }

        @Test
        @DisplayName("type tự động là ELECTRONICS")
        void testType() { assertEquals("ELECTRONICS", electronicsItem.getType()); }

        @Test
        @DisplayName("name đúng")
        void testName() { assertEquals("Laptop Gaming", electronicsItem.getName()); }

        @Test
        @DisplayName("description đúng")
        void testDescription() { assertEquals("RTX 4080", electronicsItem.getDescription()); }

        @Test
        @DisplayName("imageURL đúng")
        void testImageURL() { assertEquals("laptop.png", electronicsItem.getImageURL()); }

        @Test
        @DisplayName("instanceof Item")
        void testInstanceOf() { assertInstanceOf(Item.class, electronicsItem); }
    }

    // ══════════════════════════════════════════════════════════
    //  FashionItem
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("FashionItem")
    class FashionItemTests {

        @Test
        @DisplayName("id đúng")
        void testId() { assertEquals(2, fashionItem.getId()); }

        @Test
        @DisplayName("sellerAccountName đúng")
        void testSeller() { assertEquals("seller02", fashionItem.getSellerAccountName()); }

        @Test
        @DisplayName("type tự động là FASHION")
        void testType() { assertEquals("FASHION", fashionItem.getType()); }

        @Test
        @DisplayName("name đúng")
        void testName() { assertEquals("Áo Dài Cưới", fashionItem.getName()); }

        @Test
        @DisplayName("description đúng")
        void testDescription() { assertEquals("Lụa thêu", fashionItem.getDescription()); }

        @Test
        @DisplayName("imageURL đúng")
        void testImageURL() { assertEquals("aodai.png", fashionItem.getImageURL()); }

        @Test
        @DisplayName("instanceof Item")
        void testInstanceOf() { assertInstanceOf(Item.class, fashionItem); }
    }

    // ══════════════════════════════════════════════════════════
    //  JewelryItem
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("JewelryItem")
    class JewelryItemTests {

        @Test
        @DisplayName("id đúng")
        void testId() { assertEquals(3, jewelryItem.getId()); }

        @Test
        @DisplayName("sellerAccountName đúng")
        void testSeller() { assertEquals("seller03", jewelryItem.getSellerAccountName()); }

        @Test
        @DisplayName("type tự động là JEWELRY")
        void testType() { assertEquals("JEWELRY", jewelryItem.getType()); }

        @Test
        @DisplayName("name đúng")
        void testName() { assertEquals("Nhẫn Kim Cương", jewelryItem.getName()); }

        @Test
        @DisplayName("description đúng")
        void testDescription() { assertEquals("2 carat", jewelryItem.getDescription()); }

        @Test
        @DisplayName("imageURL đúng")
        void testImageURL() { assertEquals("ring.png", jewelryItem.getImageURL()); }

        @Test
        @DisplayName("instanceof Item")
        void testInstanceOf() { assertInstanceOf(Item.class, jewelryItem); }
    }

    // ══════════════════════════════════════════════════════════
    //  OtherItem
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("OtherItem")
    class OtherItemTests {

        @Test
        @DisplayName("id đúng")
        void testId() { assertEquals(4, otherItem.getId()); }

        @Test
        @DisplayName("sellerAccountName đúng")
        void testSeller() { assertEquals("seller04", otherItem.getSellerAccountName()); }

        @Test
        @DisplayName("type tự động là OTHER")
        void testType() { assertEquals("OTHER", otherItem.getType()); }

        @Test
        @DisplayName("name đúng")
        void testName() { assertEquals("Tranh Sơn Dầu", otherItem.getName()); }

        @Test
        @DisplayName("description đúng")
        void testDescription() { assertEquals("Nghệ thuật", otherItem.getDescription()); }

        @Test
        @DisplayName("imageURL đúng")
        void testImageURL() { assertEquals("painting.png", otherItem.getImageURL()); }

        @Test
        @DisplayName("instanceof Item")
        void testInstanceOf() { assertInstanceOf(Item.class, otherItem); }
    }

    // ══════════════════════════════════════════════════════════
    //  So sánh các subclass với nhau
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("So sánh giữa các subclass")
    class CrossItemTests {

        @Test
        @DisplayName("Tất cả 4 type khác nhau")
        void testDistinctTypes() {
            assertAll(
                () -> assertNotEquals(electronicsItem.getType(), fashionItem.getType()),
                () -> assertNotEquals(fashionItem.getType(),     jewelryItem.getType()),
                () -> assertNotEquals(jewelryItem.getType(),     otherItem.getType()),
                () -> assertNotEquals(electronicsItem.getType(), otherItem.getType())
            );
        }

        @Test
        @DisplayName("Tất cả 4 id khác nhau")
        void testDistinctIds() {
            assertAll(
                () -> assertNotEquals(electronicsItem.getId(), fashionItem.getId()),
                () -> assertNotEquals(fashionItem.getId(),     jewelryItem.getId()),
                () -> assertNotEquals(jewelryItem.getId(),     otherItem.getId())
            );
        }

        @Test
        @DisplayName("Tất cả đều instanceof Item")
        void testAllInstanceOfItem() {
            assertAll(
                () -> assertInstanceOf(Item.class, electronicsItem),
                () -> assertInstanceOf(Item.class, fashionItem),
                () -> assertInstanceOf(Item.class, jewelryItem),
                () -> assertInstanceOf(Item.class, otherItem)
            );
        }
    }
}
