# Hệ Thống Đấu Giá Trực Tuyến (Real-time Auction System) - Nhóm 6
Đây là dự án Bài tập lớn của Nhóm 6. Hệ thống mô phỏng một sàn đấu giá trực tuyến theo thời gian thực (Real-time), cho phép người dùng tạo phiên đấu giá, tham gia trả giá, và cập nhật kết quả đồng bộ ngay lập tức thông qua giao thức Socket.

🚀 Công nghệ sử dụng
Ngôn ngữ: Java (Core/JavaFX)

Kiến trúc mạng: Java Socket (Mô hình Client-Server, TCP/IP)

Cơ sở dữ liệu: MySQL

Design Pattern: Strategy Pattern, Observer Pattern, Singleton Pattern, Factory Method Pattern

🌟 Tính năng chính
Đăng nhập/Đăng ký và quản lý hồ sơ người dùng, lịch sử giao dịch.

Real-time Bidding: Trả giá theo thời gian thực, tự động broadcast giá mới nhất đến tất cả người trong phòng.

Transaction Safety: Đảm bảo tính toàn vẹn dữ liệu khi nhiều người cùng trả giá một lúc (Xử lý đa luồng & Database Transaction).

Auto-close Session: Tự động đếm ngược và chốt phiên đấu giá khi hết giờ.

Quản lý hệ thống dành cho Admin (Duyệt phiên).


### Sơ đồ 1: Cấu trúc hệ thống (UML Class Diagram)

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
    }

    class User {
        -String description
        -String avatarURL
    }

    class Admin {
        +censorSession(AuctionSession session) void
        +ban(User user) void
    }

    Person <|-- User
    Person <|-- Admin

    class AuctionSession {
        <<abstract>>
        -int id
        -LocalDateTime startTime
        -LocalDateTime endTime
        -double currentPrice
        -double bidIncrease
        -StatusOfAuction statusOfAuction
        -String sellerAccount
        -String name
        -String description
        -String imageURL
        -String highestBidderAccount
    }

    %% Các Class con của AuctionSession (Bạn hãy đổi tên theo đúng ảnh của bạn)
    class ElectronicSession {
    }
    class FashionSession {
    }
    class JewelrySession {
    }
    class OtherSession {
    }

    AuctionSession <|-- ElectronicSession
    AuctionSession <|-- FashionSession
    AuctionSession <|-- JewelrySession
    AuctionSession <|-- OtherSession

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

    %% --- PHẦN PARTICIPATION ---
    class Participation {
        -String accountName
        -int auctionSessionId
        -String roleType
    }

    %% Mối quan hệ của Participation
    User "1" --> "*" Participation : joins
    AuctionSession "1" --> "*" Participation : has
```

### Sơ đồ 2: Luồng Hệ Thống & Xử lý Kỹ Thuật (System Sequence Diagram)
```mermaid
sequenceDiagram
    autonumber
    actor Client as Client (JavaFX UI)
    participant Socket as Client SocketManager
    participant Router as Server ActionRouter (Socket Thread)
    participant BS as BiddingService
    participant Context as ParticipationContext
    participant DB as Database Connection
    participant DAOs as Tầng DAO (User, Session, Bid)
    participant Broadcaster as BroadcastService

    %% Giai đoạn 1: Client gửi Request qua Socket
    Client->>Socket: Nhấn nút Trả giá (UI)
    Note over Client, Socket: Parse request thành JSON (Gson)
    Socket->>Router: Gửi JSON: {action: "PLACE_BID", payload: {...}}

    %% Giai đoạn 2: Xử lý Đồng bộ & Logic
    Router->>BS: handleAction(jsonPayload)
    
    Note over BS: Mở Block Synchronized theo SessionId<br/>để chống nhiều người trả giá cùng mili-giây
    rect rgb(200, 220, 240)
        BS->>DAOs: Lấy thông tin Session & kiểm tra giá
        DAOs-->>BS: Trả về Session object
        
        %% Strategy & Transaction
        BS->>Context: executeAction(user, session, newPrice)
        Context->>DB: getConnection() & setAutoCommit(false)
        
        Context->>DAOs: 1. updateBalance() (Trừ tiền người mới)
        Context->>DAOs: 2. updateSession() (Cập nhật phiên)
        
        alt Có người giữ giá cũ (Bị vượt mặt)
            Context->>DAOs: 3. updateBalance() (Hoàn tiền người cũ)
        end
        
        Context->>DAOs: 4. addBid() (Lưu lịch sử)
        
        alt Transaction Thành công
            Context->>DB: commit()
            Context-->>BS: return true
        else Lỗi / Thất bại
            Context->>DB: rollback()
            Context-->>BS: return false
        end
    end

    %% Giai đoạn 3: Phản hồi & Real-time Broadcast
    alt Kết quả = true
        BS->>Router: return Success Response
        Router->>Socket: Gửi JSON: {status: "SUCCESS", ...}
        Socket-->>Client: Platform.runLater() -> Cập nhật UI cá nhân
        
        %% Bước quyết định của Real-time
        BS->>Broadcaster: broadcastNewPrice(sessionId, newPrice)
        Note over Broadcaster: Tìm tất cả Socket kết nối<br/>thuộc Session này
        Broadcaster->>Socket: Gửi JSON: {action: "UPDATE_PRICE", payload: {...}} cho TẤT CẢ clients
    else Kết quả = false
        BS->>Router: return Error Response
        Router->>Socket: Gửi JSON: {status: "ERROR", message: "..."}
        Socket-->>Client: Platform.runLater() -> Hiển thị lỗi (Cảnh báo)
    end
```
### Sơ đồ 3: Luồng Nghiệp Vụ Database (Business Logic & Data Flow)
```mermaid
sequenceDiagram
    autonumber
    actor A as User A (Seller)
    actor B as User B (Bidder)
    participant Server as Tboat Server
    participant DB_Session as Bảng auction_session
    participant DB_Part as Bảng participation
    participant DB_Bid as Bảng bid
    participant DB_User as Bảng user
    participant DB_History as Bảng history

    %% 1. A Tạo phòng
    Note over A, DB_Part: 1. A TẠO PHÒNG ĐẤU GIÁ
    A->>Server: Request Tạo Phiên
    Server->>DB_Session: INSERT 1 dòng (Tạo phiên mới)
    Server->>DB_Part: INSERT 1 dòng (accountName: A, role: SELLER)

    %% 2. B Join phòng lần 1
    Note over B, DB_Part: 2. B BẤM JOIN PHÒNG (LẦN ĐẦU)
    B->>Server: Request Tham gia (JOIN)
    Server->>DB_Part: SELECT kiểm tra B đã tồn tại trong phiên chưa?
    DB_Part-->>Server: Trả về: Chưa tồn tại
    Server->>DB_Part: INSERT 1 dòng (accountName: B, role: BIDDER)

    %% 3. B Join phòng lần 2
    Note over B, DB_Part: 3. B THOÁT RA, RỒI JOIN LẠI
    B->>Server: Request Tham gia (JOIN)
    Server->>DB_Part: SELECT kiểm tra B đã tồn tại trong phiên chưa?
    DB_Part-->>Server: Trả về: Đã tồn tại
    Server-->>Server: Bỏ qua (Không INSERT thêm)

    %% 4. B Đặt giá 500k
    Note over B, DB_Bid: 4. B BẤM ĐẶT GIÁ 500K (Lần 1)
    B->>Server: Request Đặt giá 500k
    Server->>DB_User: UPDATE trừ tiền 500k của B
    Server->>DB_Session: UPDATE current_price = 500k & highest_bidder = B
    Server->>DB_Bid: INSERT 1 dòng (bidder: B, amount: 500k)

    %% 5. B Đặt giá 600k
    Note over B, DB_Bid: 5. B BẤM ĐẶT GIÁ 600K (Lần 2)
    B->>Server: Request Đặt giá 600k
    Server->>DB_User: UPDATE hoàn 500k cũ, trừ 600k mới của B
    Server->>DB_Session: UPDATE current_price = 600k & highest_bidder = B
    Server->>DB_Bid: INSERT 1 dòng nữa (bidder: B, amount: 600k)

    %% 6. Kết thúc phiên
    Note over Server, DB_History: 6. ĐỒNG HỒ ĐẾM NGƯỢC KẾT THÚC
    Server->>Server: Timer Trigger: AUCTION_FINISHED
    Server->>DB_Session: Lấy thông tin người dẫn đầu (HighestBidder = B)
    Server->>DB_History: INSERT 1 dòng duy nhất (winner: B, final_price: 600k)
    Server->>DB_Session: UPDATE status = ENDED
```


### Bảng chia việc chi tiết cho từng thành viên

 Thành viên | Nội dung nhiệm vụ |  tiến độ |
| :--- | :--- | :--- |
|  | Ghép nối code của cả nhóm |80% |
| **Phúc** | Thiết kế giao diện trang chủ, trang nạp rút, admin, trang đấu giá | 100%|
| **Phúc** | Xử lý cập nhật UI realtime và đọc dữ liệu để hiện thị  | 100%|
| **Tâm** | Thiết kế các unit test  |60% |
| **Tâm** | Xử lý Logic Broadcast (Gửi dữ liệu thời gian thực tới tất cả Client trong phòng) |100% |
| **Tâm** | Xây dựng Giao thức truyền tin | 100% |
| **Tâm** | Xử lý Đa luồng | 70% |
| **Thái** | Thiết kế các lớp Java thuần (User, Item,...) | 100% |
| **Thái** | Xử lý Validation dữ liệu & Bắt lỗi Ngoại lệ (Exception) | 70%|
| **Phú** | Thiết kế giao diện login, register, trang Profile, History, UploadItem | 100%|
| **Phú** | Lập trình tầng DAO (Data Access Object) & Thiết kế CSDL |100% |
| **Phú và Tâm** | Thiết kế kiến trúc Socket (Server/Client) & Vẽ sơ đồ UML |100% |
| **Tâm, Thái, Phú**| Code logic Bộ đếm thời gian (Timer) & Tự động chốt phiên đấu giá |100%|
| **Thái, Phú, Tâm** | Code logic Trả giá & Xử lý đồng bộ (Synchronized chống trùng lặp) |80%|
