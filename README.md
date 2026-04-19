# Bai-tap-lon-nhom-6
làm hệ thống đấu giá


### Sơ đồ cấu trúc hệ thống (UML Class Diagram)

```mermaid
classDiagram
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
        +setBalance(double balance) void
        +setNickname(String nickname) void
    }

    class User {
        -String description
        -String avatarURL
        +setDescription(String description) void
        +setAvatar(String avatarURL) void
        +deposit(double amount) void
        +withdraw(double amount) void
        +joinSession(AuctionSession session) Participation
    }

    class Admin {
        +censorSession(AuctionSession session) void
        +ban(User user) void
    }

    Person <|-- User
    Person <|-- Admin

    %% --- PHẦN ENTITY CỐT LÕI ---
    class Item {
        -int id
        -String sellerAccount
        -String type
        -String name
        -String description
        -String imageURL
    }

    class AuctionSession {
        -int id
        -int itemId
        -LocalDateTime startTime
        -LocalDateTime endTime
        -double currentPrice
        -double bidIncrease
        -String status
    }

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

    User "1" --> "*" Item : owns
    Item "1" --> "*" AuctionSession : has
    User "1" --> "*" Bid : places
    AuctionSession "1" --> "*" Bid : receives
    User "1" --> "*" History : wins
    AuctionSession "1" --> "1" History : results in

    %% --- PHẦN XỬ LÝ LUỒNG ROLE ĐỘNG ---
    class Participation {
        -String id
        -String userAccount
        -int sessionId
        -RoleType roleType
        -TransactionRole roleBehavior
        +getRoleType() RoleType
        +executeAction() void
    }

    class RoleType {
        <<enumeration>>
        SELLER
        BIDDER
    }

    class TransactionRole {
        <<Interface>>
        +getRoleType() RoleType
    }

    class SellerRole {
        +getRoleType() RoleType
    }

    class BidderRole {
        +getRoleType() RoleType
        +placeBid(double amount) void
    }

    User "1" --> "*" Participation : joins
    AuctionSession "1" --> "*" Participation : has
    
    Participation --> "1" RoleType : identifies as
    Participation "*" --> "1" TransactionRole : delegates behavior to

    TransactionRole <|.. SellerRole : implements
    TransactionRole <|.. BidderRole : implements
```


 Thành viên | Nội dung nhiệm vụ |  tiến độ |
| :--- | :--- | :--- |
| **Phúc** | Thiết kế giao diện trang chủ | 50%|
| **Phúc** | Thiết kế trang nạp rút | 50% |
| **Phúc** | Thiết kế trang đấu giá | 0% |
| **Phúc** | Thiết kế trang duyệt của admin |50% |
| **Phúc** | Tích hợp với giao diện của Phú | 0% |
| **Phúc** | Thêm các tính năng mở rộng |0% |
| **Phúc** | Xử lý cập nhật UI realtime và đọc dữ liệu để hiện thị  | 0%|
| **Tâm** | Thiết kế kiến trúc Socket (Server/Client)  |30% |
| **Tâm** | Xử lý Logic Broadcast (Gửi dữ liệu thời gian thực tới tất cả Client trong phòng) |20% |
| **Tâm** | Xây dựng Giao thức truyền tin | 0% |
| **Tâm** | Xử lý Đa luồng | 20% |
| **Thái** | Thiết kế các lớp Java thuần (User, Item,...) | 0% |
| **Thái** | Xử lý Validation dữ liệu & Bắt lỗi Ngoại lệ (Exception) | 0%|
| **Thái** | Code logic Trả giá & Xử lý đồng bộ (Synchronized chống trùng lặp) |0%|
| **Thái** | Code logic Bộ đếm thời gian (Timer) & Tự động chốt phiên đấu giá |0%|
| **Phú** | Thiết kế giao diện đăng nhập, đăng ký | 100%|
| **Phú** | Thiết kế trang Profile |100% |
| **Phú** | Thiết kế trang lịch sử | 100% |
| **Phú** | Thiết kế trang Upload Item |80%|
| **Phú** | Lập trình tầng DAO (Data Access Object) |0% |
| **Phú** | Thiết kế CSDL (ERD) & Viết file SQL & Xây dựng lớp Database Connection |100% |
| **Phú** | Vẽ sơ đồ UML |100% |
