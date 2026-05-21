# 🏷️ Hệ Thống Đấu Giá Trực Tuyến — Nhóm 6

> Hệ thống mô phỏng sàn đấu giá trực tuyến theo thời gian thực (Real-time Auction System), cho phép người dùng tạo phiên đấu giá, tham gia trả giá và nhận cập nhật đồng bộ ngay lập tức thông qua giao thức TCP Socket.

---

## 📋 Mục lục

1. [Mô tả bài toán](#1-mô-tả-bài-toán)
2. [Công nghệ & Môi trường](#2-công-nghệ--môi-trường)
3. [Cấu trúc thư mục](#3-cấu-trúc-thư-mục)
4. [Vị trí file JAR](#4-vị-trí-file-jar)
5. [Hướng dẫn chạy](#5-hướng-dẫn-chạy)
6. [Danh sách chức năng đã hoàn thành](#6-danh-sách-chức-năng-đã-hoàn-thành)
7. [Tài liệu & Demo](#7-tài-liệu--demo)

---

## 1. Mô tả bài toán
**Bài toán đặt ra:** Khắc phục độ trễ dữ liệu và xung đột tranh chấp tài nguyên (Race Condition) khi có nhiều người dùng cùng tham gia đặt giá tại một thời điểm trong các hệ thống đấu giá truyền thống.

**Phạm vi hệ thống:** Xây dựng ứng dụng Client-Server cho phép nhiều người dùng tham gia đấu giá trực tuyến đồng thời. Hệ thống đảm bảo tính nhất quán dữ liệu khi nhiều người trả giá cùng một lúc, tự động chốt phiên khi hết thời gian, và cập nhật giá theo thời gian thực đến toàn bộ người trong phòng.
- **Client (JavaFX MVC):** Tiếp nhận tương tác, xử lý luồng hiển thị mượt mà và cập nhật trạng thái phòng đấu giá trực tiếp theo thời gian thực (Event-driven UI).
- **Server (Java Socket Multi-threading):** Đóng vai trò bộ xử lý trung tâm, điều phối các kết nối luồng, kiểm soát an toàn giao dịch tài chính (Trừ/Hoàn tiền tự động) và quản lý bộ đếm ngược tự động khóa phiên.

**Các Actor & Cây kế thừa hệ thống (Domain Entities):**
Hệ thống tuân thủ chặt chẽ nguyên lý hướng đối tượng (OOP) thông qua việc phân cấp các lớp thực thể rõ ràng, tối ưu hóa khả năng tái sử dụng mã nguồn và thể hiện tính đa hình:

* **Phân cấp Người dùng (User Hierarchy):**
  * `Person` (Abstract Class): Lớp trừu tượng cơ sở quản lý thông tin định danh cốt lõi (ID, tên, tài khoản, mật khẩu).
  * `User` (Kế thừa từ `Person`): Đại diện cho thành viên hệ thống, đóng vai trò kép linh hoạt trong quy trình nghiệp vụ: vừa là **Bidder** (tham gia phòng, đặt giá) vừa là **Seller** (đăng tải, quản lý sản phẩm).
  * `Admin` (Kế thừa từ `Person`): Người điều hành hệ thống có toàn quyền phê duyệt hoặc hủy các phiên đấu giá.

* **Phân cấp Sản phẩm đấu giá (Item Hierarchy):**
  * `Item` (Abstract Class): Định nghĩa các thuộc tính và hành vi chung của một tài sản đấu giá (tên, mô tả, giá khởi điểm, thời gian).
  * `Electronics` / `Fashion` / `Jewelry` / `Other` (Kế thừa từ `Item`): Các danh mục sản phẩm cụ thể. 
---

## 2. Công nghệ & Môi trường

| Thành phần | Chi tiết |
|---|---|
| Ngôn ngữ | Java 21 |
| Giao diện | JavaFX 21.0.1 |
| Kiến trúc mạng | Java Socket (TCP/IP, Client-Server) |
| Định dạng dữ liệu | JSON (thư viện Gson 2.10.1) |
| Cơ sở dữ liệu | MySQL 8.0 |
| Build tool | Apache Maven (Multi-module) |
| Chất lượng code | Checkstyle (Google Checks) + SpotBugs |
| Logging | SLF4J + Logback Classic 1.4.12 |
| Design Pattern | Strategy, Observer, Singleton, Factory Method |

### Yêu cầu cài đặt

- **Java Development Kit (JDK)**: Phiên bản 21 trở lên ([Tải tại đây](https://www.oracle.com/java/technologies/downloads/))
- **Cơ sở dữ liệu**: MySQL Server phiên bản 8.0 trở lên ([Tải tại đây](https://dev.mysql.com/downloads/mysql/))
- **Công cụ quản lý mã nguồn**: Apache Maven 3.9 trở lên (nếu muốn build từ source)

### Thiết lập Database

1. Đăng nhập vào môi trường quản trị MySQL của bạn.
2. Thực thi tệp script nằm tại đường dẫn server/src/main/resources/auction_database.sql để thiết lập hệ thống cơ sở dữ liệu.
3. Đồng bộ lại thông tin cấu hình tài khoản kết nối của bạn trong dự án (Mật khẩu cấu hình kết nối mặc định: 123456789).

---

## 3. Cấu trúc thư mục

```
tboat-project/ (Root POM)
├── .github/workflows/
│   └── ci.yml                  # Cấu hình CI/CD (Khởi tạo DB ảo, Verify)
├── common/                       # Module dùng chung cho cả Server và Client
│   └── src/main/java/com/tboat/
│       ├── logging/              # Cấu hình nhật ký hệ thống
│       └── models/               # Tầng Domain Models (Thực thể hệ thống)
├── server/                       # Module xử lý trung tâm (Backend)
│   ├── src/main/java/com/tboat/
│   │   ├── dao/                  # Tầng DAO (Data Access Object) - Thao tác MySQL
│   │   ├── database/             # Quản lý Connection Pool kết nối DB
│   │   ├── service/              # Tầng Business Logic nghiệp vụ chính
│   │   ├── socket/               # Quản lý kết nối mạng, định tuyến ActionRouter
│   │   └── ServerMain.java       # Entry point khởi động Server
│   └── src/main/resources/
│       └── auction_database.sql  # Script cấu trúc khởi tạo CSDL
└── client/                       # Module giao diện người dùng (Frontend)
    ├── src/main/java/com/tboat/
    │   ├── controllers/          # Tầng Presentation - Điều khiển UI JavaFX
    │   ├── socket/               # SocketManager - Duy trì cổng kết nối duy nhất
    │   ├── ucb/                  # Thuật toán tăng tốc tải tài nguyên
    │   ├── ClientApp.java        # Lớp cấu hình giao diện chính
    │   └── Launcher.java         # Lớp kích hoạt ứng dụng (Giải quyết xung đột môi trường)
    └── src/main/resources/
        ├── images                # Kho tài nguyên hình ảnh hệ thống
        ├── styles                # Các tệp cấu hình CSS làm đẹp giao diện
        └── views                 # Các tệp thiết kế giao diện độc lập định dạng .fxml

```
---

## 4. Vị trí file JAR

Sau khi build bằng lệnh `mvn package` tại thư mục gốc, các file JAR sẽ được tạo tại:

| File | Đường dẫn |
|---|---|
| Server JAR | `server/target/server-1.0-SNAPSHOT.jar` |
| Client JAR | `client/target/client-1.0-SNAPSHOT.jar` |

> Cả hai đều là **fat JAR** (đã đóng gói toàn bộ dependency bên trong), chạy trực tiếp bằng `java -jar`.

---

## 5. Hướng dẫn chạy

> ⚠️ **Bắt buộc chạy Server trước, Client sau.**

### Bước 1 — Biên dịch và đóng gói toàn bộ mã nguồn dự án
Mở Terminal tại thư mục gốc tboat-project/ và thực thi lệnh:
```bash
mvn clean package
```

### Bước 2 — Khởi động Server

```bash
java -jar server/target/server-1.0-SNAPSHOT.jar
```

Khi xuất hiện thông tin nhật ký hệ thống ghi nhận trạng thái kết nối thành công, Server đã sẵn sàng điều hướng các gói tin.
### Bước 3 — Khởi động Client

```bash
java -jar client/target/client-1.0-SNAPSHOT.jar
```

Có thể mở nhiều cửa sổ Client cùng lúc để mô phỏng nhiều người dùng.
---

## 6. Danh sách chức năng đã hoàn thành

### ✅ Chức năng cốt lõi (Hoàn thành)

| # | Chức năng | Mô tả |
|---|---|---|
| 1 | Tài khoản & Hồ sơ |Đăng nhập, đăng ký, cập nhật thông tin cá nhân bổ sung, quản lý số dư ví nạp/rút trực tuyến |
| 2 | Tạo phiên đấu giá & Quản lý sản phẩm | Đăng tải sản phẩm lên sàn đấu giá, cho phép người bán xem, chỉnh sửa và hủy phiên đấu giá |
| 3| Duyệt phiên (Admin) | Admin xem xét và phê duyệt / huỷ phiên |
| 4 | Tham gia phòng đấu giá | Join phòng, xem thông tin phiên theo thời gian thực |
| 5 | **Real-time Bidding** | Trả giá tức thì, broadcast đến toàn bộ người trong phòng |
| 6 | **Transaction Safety** | Synchronized + DB Transaction đảm bảo toàn vẹn khi nhiều người trả giá đồng thời |
| 7 | Auto-close & Hoàn tiền | Hệ thống tự động kích hoạt bộ đếm ngược, tự động hoàn trả số dư cho người bị vượt giá và chốt phiên lập lịch sử |
| 8 | Lịch sử giao dịch | Xem lại các phiên đã tham gia, đã thắng |
| 9 | Biểu đồ Bid | Trực quan hóa dữ liệu lịch sử tăng giá bằng biểu đồ dạng đường theo trục thời gian |
| 10 | Unit Test | Xây dựng hệ thống kịch bản kiểm thử đơn vị tự động hóa bảo vệ an toàn cho các hàm xử lý lõi |
| 11 | Thông báo | Gửi thông báo cho seller khi phiên được duyệt, bắt đầu và kết thúc đồng bộ xuyên suốt ứng dụng dạng Push Notification. gửi thông báo cho bidder khi mua thành công, khi bị người khác vượt bid |
| 12 | Xử lý lỗi & Ngoại lệ nghiệp vụ |Tự động chặn đặt giá thấp hơn giá hiện tại + bước giá, từ chối bid khi phiên đấu giá đã đóng hoặc kết thúc, Xử lý ngoại lệ kết nối mạng: Tự động dọn dẹp tài nguyên khi Client ngắt kết nối đột ngột |
| 13 | Anti-sniping | Nếu có bất kỳ lệnh đặt giá hợp lệ nào xuất hiện trong 15 giây cuối cùng trước khi phiên đóng, hệ thống tự động gia hạn thời gian kết thúc của phiên thêm 30 giây để đảm bảo tính cạnh tranh công bằng|
| 14 | Auto-bidding (đặt giá tự động)|
---

## 7. Tài liệu & Demo

| Tài nguyên | Link |
|---|---|
| 📄 Báo cáo PDF | *(Cập nhật sau)* |
| 🎬 Video Demo | *(Cập nhật sau)* |

---

> **Nhóm 6** — Môn Lập Trình Nâng Cao| Khoa Công nghệ Thông tin
