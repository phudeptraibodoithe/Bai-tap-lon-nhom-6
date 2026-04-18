package models;

public class Admin extends Person {

    public Admin(String account, String password, String username) {
        super(account, password, username);
    }

    public void censorSession(AuctionSession session) {
        // Code duyệt sản phẩm
        System.out.println("Sản phẩm đã được duyệt!");
    }

    public void ban(User user) {
        // Code ban tài khoản
        System.out.println("Đã khóa tài khoản: " + user.username);
    }
}