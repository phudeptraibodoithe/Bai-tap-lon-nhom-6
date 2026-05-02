# Hệ Thống Đấu Giá Trực Tuyến (Real-time Auction System) - Nhóm 6
Đây là dự án Bài tập lớn của Nhóm 6. Hệ thống mô phỏng một sàn đấu giá trực tuyến theo thời gian thực (Real-time), cho phép người dùng tạo phiên đấu giá, tham gia trả giá, và cập nhật kết quả đồng bộ ngay lập tức thông qua giao thức Socket.

🚀 Công nghệ sử dụng
Ngôn ngữ: Java (Core/JavaFX)

Kiến trúc mạng: Java Socket (Mô hình Client-Server, TCP/IP)

Cơ sở dữ liệu: MySQL

Design Pattern: Strategy Pattern, Observer Pattern, Singleton Pattern, Abstract Factory Pattern

🌟 Tính năng chính
Đăng nhập/Đăng ký và quản lý hồ sơ người dùng, lịch sử giao dịch.

Real-time Bidding: Trả giá theo thời gian thực, tự động broadcast giá mới nhất đến tất cả người trong phòng.

Transaction Safety: Đảm bảo tính toàn vẹn dữ liệu khi nhiều người cùng trả giá một lúc (Xử lý đa luồng & Database Transaction).

Auto-close Session: Tự động đếm ngược và chốt phiên đấu giá khi hết giờ.

Quản lý hệ thống dành cho Admin (Duyệt phiên).


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
        +joinSession(AuctionSession session, String roleType) Participation
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
        -String highestBidderAccount
    }

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

    %% --- PHẦN PARTICIPATION ĐÃ SỬA THEO CODE JAVA ---
    class Participation {
        -String accountName
        -int auctionSessionId
        -String roleType
    }

    %% Mối quan hệ của Participation
    User "1" --> "*" Participation : joins
    AuctionSession "1" --> "*" Participation : has
```



### Sơ đồ tuần tự (Sequence Diagram)
```mermaid
sequenceDiagram
    autonumber
    actor Client as Client / Socket Controller
    participant BS as BiddingService
    participant Context as ParticipationContext
    participant Role as BidderRole
    participant DB as Database Connection
    participant DAOs as Tầng DAO (User, Session, Bid)

    Client->>BS: placeBid(bidderAccount, sessionId, newPrice)
    
    %% Kiểm tra cơ bản
    BS->>DAOs: Lấy thông tin Session & User
    DAOs-->>BS: Trả về đối tượng (hoặc null)
    
    %% Khởi tạo Strategy
    BS->>Context: new ParticipationContext(new BidderRole())
    BS->>Context: executeAction(user, session, newPrice, ...)
    Context->>Role: execute(...)
    
    %% Bắt đầu Transaction
    Role->>DB: getConnection() & setAutoCommit(false)
    Note right of Role: Bắt đầu Transaction để<br/>đảm bảo toàn vẹn dữ liệu
    
    %% 4 Bước logic
    Role->>DAOs: 1. updateBalance() (Trừ tiền người mới)
    Role->>DAOs: 2. updateSessionPriceAndHighest() (Cập nhật phiên)
    
    alt Có người giữ giá cũ
        Role->>DAOs: 3. updateBalance() (Hoàn tiền người cũ)
    end
    
    Role->>DAOs: 4. addBid() (Lưu lịch sử)
    
    %% Xử lý kết quả Transaction
    alt Tất cả thao tác DAO thành công
        Role->>DB: commit()
        Role-->>Context: return true
    else Có bước thất bại hoặc lỗi Exception
        Role->>DB: rollback()
        Role-->>Context: return false
    end
    
    %% Trả kết quả về
    Context-->>BS: return boolean result
    BS-->>Client: Trả về trạng thái (SUCCESS/FAIL)
```



### Bảng chia việc chi tiết cho từng thành viên

 Thành viên | Nội dung nhiệm vụ |  tiến độ |
| :--- | :--- | :--- |
|  | Ghép nối code của cả nhóm |50% |
| **Phúc** | Thiết kế giao diện trang chủ, trang nạp rút | 100%|
| **Phúc** | Thiết kế giao diện của admin, trang đấu giá | 80% |
| **Phúc** | Xử lý cập nhật UI realtime và đọc dữ liệu để hiện thị  | 10%|
| **Tâm** | Thiết kế các unit test  |80% |
| **Tâm** | Xử lý Logic Broadcast (Gửi dữ liệu thời gian thực tới tất cả Client trong phòng) |80% |
| **Tâm** | Xây dựng Giao thức truyền tin | 80% |
| **Tâm** | Xử lý Đa luồng | 70% |
| **Thái** | Thiết kế các lớp Java thuần (User, Item,...) | 100% |
| **Thái** | Xử lý Validation dữ liệu & Bắt lỗi Ngoại lệ (Exception) | 70%|
| **Phú** | Thiết kế giao diện login, register, trang Profile, History, UploadItem | 100%|
| **Phú** | Lập trình tầng DAO (Data Access Object) |100% |
| **Phú** | Thiết kế CSDL (ERD) & Viết file SQL & Xây dựng lớp Database Connection |100% |
| **Phú và Tâm** | Thiết kế kiến trúc Socket (Server/Client) & Vẽ sơ đồ UML |100% |
| **Tâm và Thái**| Code logic Bộ đếm thời gian (Timer) & Tự động chốt phiên đấu giá |60%|
| **Thái, Phú, Tâm** | Code logic Trả giá & Xử lý đồng bộ (Synchronized chống trùng lặp) |70%|
