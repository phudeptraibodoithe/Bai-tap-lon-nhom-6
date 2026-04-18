package models;

public class User extends Person {
    // Attributes
    private int id;
    private String description;
    private String avatarURL;
    private double balance;

    // Constructor
    public User(String account, String password, String username) {
        super(account, password, username);
        this.balance = 0.0;
    }

    // Methods
    public void setDescription(String description) {
        this.description = description;
    }

    public void setAvatar(String avatarURL) {
        this.avatarURL = avatarURL;
    }

    public void deposit(double amount) {
        if (amount > 0) {
            this.balance += amount;
        }
    }

    public void withdraw(double amount) {
        if (amount > 0 && this.balance >= amount) {
            this.balance -= amount;
        }
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public void setUsername(String username) {
        super.setUsername(username);
    }

    // Hàm này trả về một đối tượng Participation (Hồ sơ tham gia phiên đấu giá)
    public Participation joinSession(AuctionSession session) {
        // Logic thực tế sẽ tạo và trả về đối tượng Participation ở đây
        // Ví dụ tạm thời trả về null để không báo lỗi cú pháp
        return null; 
    }
}