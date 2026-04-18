# Bai-tap-lon-nhom-6
làm hệ thống đấu giá


### Sơ đồ cấu trúc hệ thống (UML Class Diagram)

```mermaid
classDiagram
    %% --- PHẦN CLASS KẾ THỨC CƠ BẢN ---
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
        -String id
        -String email
        +deposit(double amount) void
        +withdraw(double amount) void
        +joinTransaction(BidTransaction transaction) Participation
    }

    class Admin {
        +censorTransaction(BidTransaction transaction) void
        +ban(User user) void
    }

    Person <|-- User
    Person <|-- Admin

    %% --- PHẦN ENTITY CỐT LÕI ---
    class Item {
        -String id
        -String type
        -String name
        -String description
        -String image
    }

    class BidTransaction {
        -String id
        -LocalDateTime timeStamp
        -double currentPrice
        -double bidIncrease
        -String status
    }

    User "1" --> "*" Item : owns
    BidTransaction "*" --> "1" Item : auctions

    %% --- PHẦN XỬ LÝ LUỒNG ROLE ĐỘNG ---
    class Participation {
        -String id
        -User user
        -BidTransaction transaction
        -RoleType roleType
        -TransactionRole roleBehavior
        -LocalDateTime joinedAt
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
        +acceptBid() void
        +cancelTransaction() void
    }

    class BidderRole {
        +placeBid(double amount) void
        +retractBid() void
    }

    User "1" --> "*" Participation : joins
    BidTransaction "1" --> "*" Participation : has
    
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
| **Phú** | Xây dựng lớp Database Connection |0% |
| **Phú** | Thiết kế CSDL (ERD) & Viết file SQL |0% |
