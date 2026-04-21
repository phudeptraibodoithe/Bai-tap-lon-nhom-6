package models;

public class SellerRole implements TransactionRole {
    
    @Override
    public RoleType getRoleType() {
        return RoleType.SELLER;
    }
}