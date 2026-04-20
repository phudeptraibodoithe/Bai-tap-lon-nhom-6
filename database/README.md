# Bai-tap-lon-nhom-6
làm hệ thống đấu giá

```mermaid
classDiagram
    %% --- PHẦN CLASS KẾ THỪA CƠ BẢN ---
    class Person {
        <<abstract>>
        -String account
        -String password
        -String username
        -double balance
        +getUsername() String
        +setBalance(double balance) void
    }

    class User {
        -int id
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
        -String type
        -String name
        -String description
        -String imageURL
    }

    class AuctionSession {
        -int id
        -LocalDateTime startTime
        -LocalDateTime endTime
        -double currentPrice
        -double bidIncrease
        -String status
    }

    class Bid {
        -int id
        -double bidAmount
        -LocalDateTime bidTime
    }

    class History {
        -int id
        -double finalPrice
        -LocalDateTime completedAt
    }

    %% Liên kết giữa các thực thể cốt lõi
    User "1" --> "*" Item : owns
    Item "1" --> "*" AuctionSession : has
    
    User "1" --> "*" Bid : places
    AuctionSession "1" --> "*" Bid : receives
    
    User "1" --> "*" History : wins
    AuctionSession "1" --> "1" History : results in

    %% --- PHẦN XỬ LÝ LUỒNG ROLE ĐỘNG ---
    class Participation {
        -String id
        -User user
        -AuctionSession session
        -RoleType roleType
        -TransactionRole roleBehavior
        -LocalDateTime joinedAt
        +executeAction() void
    }

    class RoleType {
        <<enumeration>>
        BIDDER
    }

    class TransactionRole {
        +getRoleType() RoleType
        +placeBid(double amount) void
        +retractBid() void
    }

    User "1" --> "*" Participation : joins
    AuctionSession "1" --> "*" Participation : has
    
    Participation --> "1" RoleType : identifies as
    Participation "*" --> "1" TransactionRole : delegates behavior to
```

 Thành viên | Nội dung nhiệm vụ |  tiến độ |
| :--- | :--- | :--- |
| **Phúc** | Thiết kế giao diện trang chủ | 0%|
| **Phúc** | Thiết kế trang nạp rút | 0% |
| **Phúc** | Thiết kế trang đấu giá | 0% |
| **Phúc** | Thiết kế trang duyệt của admin |0% |
| **Phúc** | Tích hợp với giao diện của Phú | 0% |
| **Phúc** | Thêm các tính năng mở rộng |0% |
| **Phúc** | Xử lý cập nhật UI realtime và đọc dữ liệu để hiện thị  | 0%|
| **Tâm** | Thiết kế kiến trúc Socket (Server/Client)  |0% |
| **Tâm** | Xử lý Logic Broadcast (Gửi dữ liệu thời gian thực tới tất cả Client trong phòng) |0% |
| **Tâm** | Xây dựng Giao thức truyền tin | 0% |
| **Tâm** | Xử lý Đa luồng | 0% |
| **Thái** | Thiết kế các lớp Java thuần (User, Item,...) | 0% |
| **Thái** | Xử lý Validation dữ liệu & Bắt lỗi Ngoại lệ (Exception) | 0%|
| **Thái** | Code logic Trả giá & Xử lý đồng bộ (Synchronized chống trùng lặp) |0%|
| **Thái** | Code logic Bộ đếm thời gian (Timer) & Tự động chốt phiên đấu giá |0%|
| **Phú** | Thiết kế giao diện đăng nhập, đăng ký | 100%|
| **Phú** | Thiết kế trang Profile |100% |
| **Phú** | Thiết kế trang lịch sử | 100% |
| **Phú** | Thiết kế trang Upload Item |80%|
| **Phú** | Lập trình tầng DAO (Data Access Object) |0% |
| **Phú** | Xây dựng lớp Database Connection |0% |
| **Phú** | Thiết kế CSDL (ERD) & Viết file SQL |0% |