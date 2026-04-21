package models;

public class Participation {
    private String id;
    private int userId;
    private int sessionId;
    private RoleType roleType;
    private TransactionRole roleBehavior; // Chứa logic tương ứng với Role

    public Participation(String id, int userId, int sessionId, TransactionRole roleBehavior) {
        this.id = id;
        this.userId = userId;
        this.sessionId = sessionId;
        this.roleBehavior = roleBehavior;
        this.roleType = roleBehavior.getRoleType(); // Lấy trực tiếp từ behavior
    }

    public RoleType getRoleType() {
        return roleType;
    }

    public void executeAction() {
        // Delegate (ủy quyền) hành vi thực thi xuống cho interface
        // Nếu roleBehavior là BidderRole, nó có thể ép kiểu để gọi hàm placeBid()
        if (roleBehavior instanceof BidderRole) {
            System.out.println("Người này là Bidder, chuẩn bị đặt giá...");
            // Ép kiểu để gọi hàm riêng của BidderRole
            // ((BidderRole) roleBehavior).placeBid(100.0);
        } else if (roleBehavior instanceof SellerRole) {
            System.out.println("Người này là Seller, đang theo dõi phiên đấu giá...");
        }
    }
}