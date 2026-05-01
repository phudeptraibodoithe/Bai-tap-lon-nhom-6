package com.tboat.socket;

import com.tboat.dao.*;
import com.tboat.models.*;
import com.tboat.service.*;
import com.tboat.utils.ResponseCode;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.stream.Collectors;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private String clientId = "Guest";
    private AuctionRoom currentRoom;
    private UserManager userManager = UserManager.getInstance();
    private UserDAO userDAO = new UserDAO();
    private HistoryBidDAO historyDAO = new HistoryBidDAO();
    private AuctionSessionDAO auctionDAO = new AuctionSessionDAO();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            this.out = new PrintWriter(socket.getOutputStream(), true);
            out.println("SERVER_READY|Chào mừng bạn đến với hệ thống đấu giá TBoat!");
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                handleCommand(inputLine.trim());
            }
        } catch (IOException e) {
            System.out.println("Kết nối với " + clientId + " bị ngắt.");
        } finally {
            cleanUp();
        }
    }

    private void handleCommand(String input) {
        String[] parts = input.split("\\|", -1);
        if (parts.length == 0) return;
        String action = parts[0].toUpperCase();
        switch (action) {
            case "LOGIN": // LOGIN|user|pass
                if (parts.length < 3) {
                    out.println("LOGIN_FAILED|MISSING_PARAMETERS");
                    break;
                }
                String username = parts[1];
                String password = parts[2];
                ResponseCode loginRes = userManager.login(username, password, this);
                switch (loginRes) {
                    case SUCCESS:
                        this.clientId = username;
                        User user = userDAO.getUser(this.clientId);
                        if (user != null) {
                            String nick = (user.getNickname() != null) ? user.getNickname() : "Người dùng";
                            String avt = (user.getAvatarURL() != null) ? user.getAvatarURL() : "default.png";
                            String bio = (user.getDescription() != null) ? user.getDescription() : "";

                            out.println("LOGIN_SUCCESS|" + nick + "|" + user.getBalance() + "|" + avt + "|" + bio);
                        } else {
                            out.println("LOGIN_FAILED|DATABASE_ERROR");
                        }
                        break;
                    case NOT_FOUND:
                        out.println("LOGIN_FAILED|USER_NOT_FOUND");
                        break;
                    case WRONG_PASSWORD:
                        out.println("LOGIN_FAILED|WRONG_PASSWORD");
                        break;
                    case ALREADY_LOGGED_IN:
                        out.println("LOGIN_FAILED|ALREADY_LOGGED_IN");
                        break;
                    default:
                        out.println("LOGIN_FAILED|UNKNOWN_ERROR");
                        break;
                }
                break;
            case "REGISTER": // REGISTER|user|pass|nickname
                if (parts.length < 4) return;
                ResponseCode regRes = userManager.register(parts[1], parts[2], parts[3]);
                out.println("REGISTER_RESULT|" + regRes.name());
                break;

            case "PROFILE":
                // Khai báo rõ ràng kiểu dữ liệu User ở đây
                User userProfile = userDAO.getUser(clientId);
                if (userProfile != null) {
                    out.println("PROFILE_INFO|" + userProfile.getAccountName() + "|" + userProfile.getNickname() + "|" + userProfile.getBalance()+"|" + userProfile.getAvatarURL()+"|" + userProfile.getDescription());
                }
                break;

            case "DETAIL_AUCTION": // DETAIL_AUCTION|id
                if (parts.length < 2) return;
                AuctionSession session = auctionDAO.getAuctionById(Integer.parseInt(parts[1]));
                if (session != null) {
                    out.println("AUCTION_DETAIL|" + session.getName() + "|" + session.getDescription() + "|" + session.getCurrentPrice());
                } else {
                    out.println("ERROR|Không tìm thấy phiên đấu giá.");
                }
                break;


            case "LIST_AVAILABLE":
                List<AuctionSession> sessions = auctionDAO.getAvailableAuctions();
                String listData = sessions.stream()
                        .map(s -> s.getId() + ":" + s.getName() + ":" + s.getCurrentPrice())
                        .collect(Collectors.joining(";"));
                out.println("AVAILABLE_AUCTIONS|" + (listData.isEmpty() ? "NONE" : listData));
                break;

            case "JOIN":
                if (parts.length < 2) {
                    out.println("ERROR|Thiếu mã phòng.");
                    break;
                }

                String roomName = parts[1];
                // Lấy instance phòng từ Manager
                AuctionRoom targetRoom = AuctionManager.getInstance().getRoom(roomName);

                if (targetRoom != null) {
                    // Nếu đang ở phòng cũ thì thoát ra trước
                    if (currentRoom != null) {
                        currentRoom.removeSubscriber(this);
                    }

                    // Gán phòng mới và thêm người nghe
                    currentRoom = targetRoom;
                    currentRoom.addSubscriber(this);

                    out.println("JOIN_SUCCESS|Bạn đã vào phòng: " + roomName);
                    // Có thể gửi thêm giá hiện tại cho người mới vào biết
                    out.println("ROOM_INFO|Giá hiện tại: " + currentRoom.getCurrentPrice());
                } else {
                    // Trả về lỗi nếu không tìm thấy phòng (VD: gõ sai ID)
                    out.println("ERROR|Phòng đấu giá [" + roomName + "] không tồn tại.");
                }
                break;

            case "BID":
                if (clientId.equals("Guest") || currentRoom == null) {
                    out.println("ERROR|Hãy đăng nhập và tham gia phòng trước.");
                    break;
                }
                try {
                    double price = Double.parseDouble(parts[1]);

                    // FIX: Thêm kiểu dữ liệu "User" vào trước biến bidUser
                    User bidUser = userDAO.getUser(clientId);
                    if (bidUser == null || bidUser.getBalance() < price) {
                        out.println("BID_FAILED|Số dư không đủ để đặt mức giá này.");
                        break;
                    }

                    if (currentRoom.placeBid(price, clientId)) {
                        try {
                            int sId = Integer.parseInt(currentRoom.getRoomName());
                            historyDAO.updateBidLeader(sId, clientId, price);
                        } catch (Exception e) { /* Log error */ }

                        currentRoom.broadcast("NEW_BID|" + price + "|" + clientId);
                    } else {
                        out.println("BID_FAILED|Giá đặt phải cao hơn giá hiện tại.");
                    }
                } catch (NumberFormatException e) {
                    out.println("ERROR|Định dạng giá không hợp lệ.");
                }
                break;

            case "GET_BID": // GET_BID|sessionId (Xem ai đang dẫn đầu phiên đó)
                if (parts.length < 2) return;
                String leader = historyDAO.getLeadBidder(Integer.parseInt(parts[1]));
                out.println("BID_LEADER|" + (leader != null ? leader : "Chưa có ai"));
                break;

            case "TRANSACTION": // TRANSACTION|amount
                try {
                    double amount = Double.parseDouble(parts[1]);
                    if (userDAO.updateBalance(this.clientId, amount)) {
                        out.println("TRANSACTION_SUCCESS|" + amount);
                    } else {
                        out.println("TRANSACTION_FAILED|Số dư không đủ hoặc lỗi hệ thống");
                    }
                } catch (NumberFormatException e) {
                    out.println("ERROR|Định dạng tiền không hợp lệ");
                }
                break;

            case "UPDATE_PROFILE":
                if (parts.length < 3) {
                    out.println("UPDATE_PROFILE_ERROR|INVALID_FORMAT");
                    break;
                }
                String newDesc = parts[1];
                String newAvt = parts[2];
                User updateUser = userDAO.getUser(this.clientId);
                if (updateUser != null) {
                    updateUser.setDescription(newDesc);
                    updateUser.setAvatar(newAvt);

                    if (userDAO.updateUser(updateUser)) {
                        out.println("UPDATE_PROFILE_SUCCESS");
                    } else {
                        out.println("UPDATE_PROFILE_ERROR");
                    }
                }
                break;

            case "GET_HISTORY":
                String targetAccount = (parts.length > 1) ? parts[1] : this.clientId;
                List<History> historyList = historyDAO.getHistoryByAccount(targetAccount);

                StringBuilder sb = new StringBuilder("HISTORY_RES");
                for (History h : historyList) {
                    // Nối thêm thông tin: ID|Giá|Thời gian
                    sb.append("|").append(h.getAuctionSessionId())
                            .append(";").append(h.getFinalPrice())
                            .append(";").append(h.getCompletedAt().toString());
                }
                // Trả về đúng format bản dưới
                out.println(sb.toString());
                break;

            case "LOGOUT":
                if (!this.clientId.equals("Guest")) {
                    userManager.logout(this.clientId); // Xóa đúng user hiện tại
                    System.out.println("[Server]: User " + this.clientId + " đã đăng xuất.");
                    this.clientId = "Guest";
                }
                out.println("LOGOUT_SUCCESS|Hẹn gặp lại!");
                if (currentRoom != null) currentRoom.removeSubscriber(this);
                break;

            case "GET_PENDING_ITEMS":
                // Giả định AuctionSessionDAO của bạn có hàm getPendingAuctions() lấy các phiên chờ duyệt
                List<AuctionSession> pendingList = auctionDAO.getPendingAuctions();
                StringBuilder res = new StringBuilder("PENDING_ITEMS_RESULT");

                for(AuctionSession s : pendingList) {
                    // Nối dữ liệu theo đúng chuẩn AdminController đang chờ: ID,Name,StartPrice,Jump,Seller
                    res.append("|").append(s.getId()).append(",")
                            .append(s.getName()).append(",")
                            .append(s.getCurrentPrice()).append(",")
                            .append(s.getBidIncrease()).append(",")
                            .append(s.getSellerAccountName()); // Hoặc s.getCreator() tùy cách đặt tên
                }
                out.println(res.toString());
                break;

            case "APPROVE_ITEM":
                if (parts.length < 2) break;
                int approveId = Integer.parseInt(parts[1]);

                // Gọi đúng tên hàm và truyền vào Enum của bạn
                if (auctionDAO.updateSessionStatus(approveId, StatusOfAuction.ACTIVE)) {
                    out.println("APPROVE_SUCCESS|" + approveId);
                } else {
                    out.println("ERROR|Lỗi cơ sở dữ liệu khi duyệt sản phẩm");
                }
                break;

            case "REJECT_ITEM":
                if (parts.length < 2) break;
                int rejectId = Integer.parseInt(parts[1]);

                // Gọi đúng tên hàm và truyền vào Enum của bạn
                if (auctionDAO.updateSessionStatus(rejectId, StatusOfAuction.REJECTED)) {
                    out.println("REJECT_SUCCESS|" + rejectId);
                } else {
                    out.println("ERROR|Lỗi cơ sở dữ liệu khi từ chối sản phẩm");
                }
                break;
            default:
                out.println("UNKNOWN_COMMAND|Lệnh không hợp lệ.");
        }
    }

    // Thêm từ khóa synchronized để đảm bảo tại một thời điểm chỉ có 1 luồng được ghi vào 'out'
    public synchronized void sendMessage(String msg) {
        if (out != null) {
            out.println(msg);
            out.flush(); // Đảm bảo dữ liệu được đẩy đi ngay lập tức
        }
    }

    private void cleanUp() {
        try {
            // QUAN TRỌNG: Logout user khỏi danh sách online
            if (clientId != null && !clientId.equals("Guest")) {
                userManager.logout(clientId);
                System.out.println("[Server]: Đã giải phóng tài nguyên cho user: " + clientId);
            }

            if (currentRoom != null) currentRoom.removeSubscriber(this);
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}