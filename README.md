# Bai-tap-lon-nhom-6
làm hệ thống đấu giá


### Sơ đồ cấu trúc hệ thống (UML Class Diagram)

```mermaid
classDiagram
    %% --- PHẦN ENUM ---
    class StatusOfAuction {
        <<enumeration>>
        NOT_STARTED
        ONGOING
        ENDED
        PENDING
        CANCELED
    }

    class RoleType {
        <<enumeration>>
        SELLER
        BIDDER
    }

    %% --- PHẦN CLASS KẾ THỪA CƠ BẢN ---
    class Person {
        <<abstract>>
        -String accountName
        -String nickname
        -String password
        -double balance
        +getAccountName() String
        +getNickname() String
        +getBalance() double
        +setNickname(String nickname) void
        +deposit(double amount) void
        +withdraw(double amount) void
    }

    class User {
        -String description
        -String avatarURL
        +setDescription(String description) void
        +setAvatar(String avatarURL) void
        +joinSession(AuctionSession session, RoleType role) Participation
    }

    class Admin {
        +censorSession(AuctionSession session) void
        +ban(User user) void
    }

    Person <|-- User
    Person <|-- Admin

    %% --- PHẦN ENTITY CỐT LÕI ---
    class AuctionSession {
        -int id
        -LocalDateTime startTime
        -LocalDateTime endTime
        -double currentPrice
        -double bidIncrease
        -StatusOfAuction statusOfAuction
        -String sellerAccount
        -String type
        -String name
        -String description
        -String imageURL
    }

    %% Kết nối AuctionSession với Status
    AuctionSession --> StatusOfAuction : has status

    class Bid {
        -int id
        -int auctionSessionId
        -String bidderAccount
        -double bidAmount
        -LocalDateTime bidTime
    }

    class History {
        -int auctionSessionId
        -String winnerAccount
        -double finalPrice
        -LocalDateTime completedAt
    }

    User "1" -- "*" AuctionSession : creates/owns
    User "1" --> "*" Bid : places
    AuctionSession "1" --> "*" Bid : receives
    User "1" --> "*" History : wins
    AuctionSession "1" --> "1" History : results in

    %% --- PHẦN XỬ LÝ LUỒNG ROLE ĐỘNG (ĐÃ TỐI ƯU ĐA HÌNH) ---
    class Participation {
        -String id
        -String userAccount
        -int sessionId
        -RoleType roleType
        -TransactionRole roleBehavior
        +getRoleType() RoleType
        +executeAction(AuctionSession session, User user, Object[] args) void
    }

    class TransactionRole {
        <<Interface>>
        +getRoleType() RoleType
        +performAction(AuctionSession session, User user, Object[] args) void
    }

    class SellerRole {
        +getRoleType() RoleType
        +performAction(AuctionSession session, User user, Object[] args) void
    }

    class BidderRole {
        +getRoleType() RoleType
        +performAction(AuctionSession session, User user, Object[] args) void
    }

    %% Mối quan hệ của Participation
    User "1" --> "*" Participation : joins
    AuctionSession "1" --> "*" Participation : has
    
    Participation --> "1" RoleType : identifies as
    Participation "*" --> "1" TransactionRole : delegates behavior to

    TransactionRole <|.. SellerRole : implements
    TransactionRole <|.. BidderRole : implements
```


 Thành viên | Nội dung nhiệm vụ |  tiến độ |
| :--- | :--- | :--- |
|  | Ghép nối code của cả nhóm |30% |
| **Phúc** | Thiết kế giao diện trang chủ | 80%|
| **Phúc** | Thiết kế trang nạp rút | 100% |
| **Phúc** | Thiết kế trang đấu giá | 50% |
| **Phúc** | Thiết kế trang duyệt của admin |50% |
| **Phúc** | Tích hợp với giao diện của Phú | 80% |
| **Phúc** | Thêm các tính năng mở rộng |50% |
| **Phúc** | Xử lý cập nhật UI realtime và đọc dữ liệu để hiện thị  | 0%|
| **Tâm** | Thiết kế kiến trúc Socket (Server/Client)  |80% |
| **Tâm** | Xử lý Logic Broadcast (Gửi dữ liệu thời gian thực tới tất cả Client trong phòng) |50% |
| **Tâm** | Xây dựng Giao thức truyền tin | 80% |
| **Tâm** | Xử lý Đa luồng | 50% |
| **Thái** | Thiết kế các lớp Java thuần (User, Item,...) | 100% |
| **Thái** | Xử lý Validation dữ liệu & Bắt lỗi Ngoại lệ (Exception) | 50%|
| **Thái** | Code logic Trả giá & Xử lý đồng bộ (Synchronized chống trùng lặp) |50%|
| **Thái** | Code logic Bộ đếm thời gian (Timer) & Tự động chốt phiên đấu giá |30%|
| **Phú** | Thiết kế giao diện đăng nhập, đăng ký | 100%|
| **Phú** | Thiết kế trang Profile |100% |
| **Phú** | Thiết kế trang lịch sử | 100% |
| **Phú** | Thiết kế trang Upload Item |100%|
| **Phú** | Lập trình tầng DAO (Data Access Object) |100% |
| **Phú** | Thiết kế CSDL (ERD) & Viết file SQL & Xây dựng lớp Database Connection |100% |
| **Phú** | Vẽ sơ đồ UML |100% |
