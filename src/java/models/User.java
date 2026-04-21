package models;

public class User extends Person {
    // Attributes
    private String description;
    private String avatarURL;

    // Constructor
    public User(String accountName, String password, String nickname,double balance,String description,String avatarURL) {
        super(accountName, password, nickname,balance);
        this.avatarURL=avatarURL;
        this.description=description;
    }

    // Setters
    public void setDescription(String description) {
        this.description = description;
    }
    public void setAvatar(String avatarURL) {
        this.avatarURL = avatarURL;
    }
    
    // Getters
    public String getDescription() {
        return description;
    }
    public String getAvatarURL() {
        return avatarURL;
    }

    // Methods
    public void deposit(double amount) {
        if (amount > 0) {
            this.balance += amount;
        }
    }

    // Hàm này trả về một đối tượng Participation (Hồ sơ tham gia phiên đấu giá)
    public Participation joinSession(AuctionSession session) {
        // Logic thực tế sẽ tạo và trả về đối tượng Participation ở đây
        // Ví dụ tạm thời trả về null để không báo lỗi cú pháp
        return null; 
    }
}