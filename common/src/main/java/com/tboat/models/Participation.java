package com.tboat.models;

public class Participation {
    private String id;
    private String userAccountName;
    private int sessionId;
    private RoleType roleType;
    private TransactionRole roleBehavior; // Chứa logic tương ứng với Role

    public Participation(String id, String userAccountName, int sessionId, TransactionRole roleBehavior) {
        this.id = id;
        this.userAccountName = userAccountName;
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
            System.out.println("Set price: ");
            // Ép kiểu để gọi hàm riêng của BidderRole
        } else if (roleBehavior instanceof SellerRole) {
            System.out.println("Observation only!");
        }
    }
}