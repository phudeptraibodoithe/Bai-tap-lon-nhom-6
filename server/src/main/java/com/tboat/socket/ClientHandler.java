package com.tboat.socket;

import com.tboat.dao.*;
import com.tboat.models.*;
import com.tboat.service.*;
import com.tboat.utils.ResponseCode;

import java.io.*;
import java.net.*;
import java.util.Arrays;
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
    private static final List<String> PUBLIC_ACTIONS = Arrays.asList(
            "LOGIN", "REGISTER", "LIST_AVAILABLE", "GET_PENDING_ITEMS", "APPROVE_ITEM", "REJECT_ITEM"
    );
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

        if (!PUBLIC_ACTIONS.contains(action) && clientId.equals("Guest")) {
            out.println("ERROR|Vui lòng đăng nhập để thực hiện chức năng này.");
            return;
        }

        switch (action) {
            case "LOGIN": // Format: LOGIN|username|password
                if (parts.length < 3) {
                    out.println("LOGIN_FAILED|MISSING_PARAMETERS");
                    break;
                }
                String username = parts[1];
                String password = parts[2];
                if (username.equals("admin") && password.equals("admin")) {
                    this.clientId = "admin";
                    out.println("LOGIN_ADMIN_SUCCESS");
                    break;
                }
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
                            userManager.logout(this.clientId);
                            this.clientId = "Guest";
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

            case "REGISTER": // Format: REGISTER|username|password|nickname
                if (parts.length < 4) return;
                ResponseCode regRes = userManager.register(parts[1], parts[2], parts[3]);
                out.println("REGISTER_RESULT|" + regRes.name());
                break;

            case "PROFILE": // Format: PROFILE
                User userProfile = userDAO.getUser(clientId);
                if (userProfile != null) {
                    out.println("PROFILE_INFO|" + userProfile.getAccountName() + "|" + userProfile.getNickname() + "|" + userProfile.getBalance() + "|" + userProfile.getAvatarURL() + "|" + userProfile.getDescription());
                }
                break;

            case "DETAIL_AUCTION": // Format: DETAIL_AUCTION|auctionId
                if (parts.length < 2) return;
                AuctionSession session = auctionDAO.getAuctionById(Integer.parseInt(parts[1]));
                if (session != null) {
                    out.println("AUCTION_DETAIL|" + session.getName() + "|" + session.getDescription() + "|" + session.getCurrentPrice());
                } else {
                    out.println("ERROR|Không tìm thấy phiên đấu giá.");
                }
                break;

            case "LIST_AVAILABLE": // Format: LIST_AVAILABLE
                List<AuctionSession> sessions = auctionDAO.getAvailableAuctions();
                String listData = sessions.stream()
                        .map(s -> s.getId() + ":" + s.getName() + ":" + s.getCurrentPrice())
                        .collect(Collectors.joining(";"));
                out.println("AVAILABLE_AUCTIONS|" + (listData.isEmpty() ? "NONE" : listData));
                break;

            case "JOIN": // Format: JOIN|roomName
                if (parts.length < 2) {
                    out.println("ERROR|Thiếu mã phòng.");
                    break;
                }
                String roomName = parts[1];
                AuctionRoom targetRoom = AuctionManager.getInstance().getRoom(roomName);

                if (targetRoom != null) {
                    if (currentRoom != null) {
                        currentRoom.removeSubscriber(this);
                    }
                    currentRoom = targetRoom;
                    currentRoom.addSubscriber(this);

                    out.println("JOIN_SUCCESS|Bạn đã vào phòng: " + roomName);
                    out.println("ROOM_INFO|Giá hiện tại: " + currentRoom.getCurrentPrice());
                } else {
                    out.println("ERROR|Phòng đấu giá [" + roomName + "] không tồn tại.");
                }
                break;

            case "BID": // Format: BID|price
                if (clientId.equals("Guest") || currentRoom == null) {
                    out.println("ERROR|Hãy đăng nhập và tham gia phòng trước.");
                    break;
                }

                try {
                    double price = Double.parseDouble(parts[1]);
                    int sId = Integer.parseInt(currentRoom.getRoomName());

                    double currentPriceInMemory = currentRoom.getCurrentPrice();
                    if (price <= currentPriceInMemory) {
                        out.println("BID_FAILED|Mức giá phải cao hơn: " + currentPriceInMemory);
                        break;
                    }

                    double currentBalance = userDAO.getBalance(clientId);
                    if (currentBalance < price) {
                        out.println("BID_FAILED|Số dư không đủ.");
                        break;
                    }

                    String prevBidder = currentRoom.getLastBidder();
                    ResponseCode res = historyDAO.updateBidLeader(sId, clientId, price, prevBidder, currentPriceInMemory);
                    if (res == ResponseCode.SUCCESS) {
                        currentRoom.placeBid(price, clientId);
                        currentRoom.broadcast("NEW_BID|" + price + "|" + clientId);
                        out.println("BID_SUCCESS|Bạn đang dẫn đầu!");
                    } else if (res == ResponseCode.BID_FAILED || res == ResponseCode.BID_FAILED) {
                        out.println("BID_FAILED|Giá của bạn đã bị người khác vượt qua trước. Hãy f5 lại!");
                    } else if (res == ResponseCode.INSUFFICIENT_BALANCE) {
                        out.println("BID_FAILED|Số dư thực tế không đủ.");
                    } else {
                        out.println("ERROR|Lỗi hệ thống khi ghi nhận mức giá.");
                    }
                } catch (Exception e) {
                    out.println("ERROR|Lệnh đặt giá không hợp lệ.");
                }
                break;

            case "POST_ITEM": // Format: POST_ITEM|name|description|type|imageURL|startPrice|bidIncrease|durationSeconds
                try {
                    String name = parts[1];
                    String desc = parts[2];
                    String type = parts[3];
                    String img = parts[4];
                    double startPrice = Double.parseDouble(parts[5]);
                    double bidInc = Double.parseDouble(parts[6]);
                    int duration = Integer.parseInt(parts[7]);

                    AuctionSession newSession = new AuctionSession();
                    newSession.setName(name);
                    newSession.setDescription(desc);
                    newSession.setType(type);
                    newSession.setImageURL(img);
                    newSession.setCurrentPrice(startPrice);
                    newSession.setBidIncrease(bidInc);
                    newSession.setSellerAccountName(this.clientId);
                    newSession.setStatusOfAuction(StatusOfAuction.PENDING);
                    newSession.setStartTime(java.time.LocalDateTime.now());
                    newSession.setEndTime(java.time.LocalDateTime.now().plusSeconds(duration));

                    int generatedId = auctionDAO.addAuctionSession(newSession);

                    if (generatedId > 0) {
                        AuctionManager.getInstance().createRoom(String.valueOf(generatedId), startPrice);
                        out.println("POST_SUCCESS|" + generatedId);
                        System.out.println("[Server]: User " + clientId + " đã mở phiên mới ID: " + generatedId);
                    }
                } catch (Exception e) {
                    out.println("ERROR|Dữ liệu đăng tải không đúng định dạng.");
                }
                break;

            case "GET_BID": // Format: GET_BID|sessionId
                if (parts.length < 2) return;
                String leader = auctionDAO.getAuctionById(Integer.parseInt(parts[1])).getHighestBidderAccount();
                out.println("BID_LEADER|" + (leader != null ? leader : "Chưa có ai"));
                break;

            case "TRANSACTION": // Format: TRANSACTION|amount
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

            case "UPDATE_PROFILE": // Format: UPDATE_PROFILE|newDescription|newAvatarURL
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

            case "GET_HISTORY": // Format: GET_HISTORY  hoặc  GET_HISTORY|targetAccount
                String targetAccount = (parts.length > 1) ? parts[1] : this.clientId;
                List<History> historyList = historyDAO.getHistoryByAccount(targetAccount);

                StringBuilder sb = new StringBuilder("HISTORY_RES");
                for (History h : historyList) {
                    sb.append("|").append(h.getAuctionSessionId())
                            .append(";").append(h.getFinalPrice())
                            .append(";").append(h.getCompletedAt().toString());
                }
                out.println(sb.toString());
                break;

            case "GET_PENDING_ITEMS":
                List<AuctionSession> pendingList = auctionDAO.getPendingAuctions();
                StringBuilder res = new StringBuilder("PENDING_ITEMS_RESULT");

                for(AuctionSession s : pendingList) {
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
                if (auctionDAO.updateSessionStatus(approveId, StatusOfAuction.NOT_STARTED)) {
                    out.println("APPROVE_SUCCESS|" + approveId);
                } else {
                    out.println("ERROR|Lỗi cơ sở dữ liệu khi duyệt sản phẩm");
                }
                break;

            case "REJECT_ITEM":
                if (parts.length < 2) break;
                int rejectId = Integer.parseInt(parts[1]);

                // Gọi đúng tên hàm và truyền vào Enum của bạn
                if (auctionDAO.updateSessionStatus(rejectId, StatusOfAuction.CANCELED)) {
                    out.println("REJECT_SUCCESS|" + rejectId);
                } else {
                    out.println("ERROR|Lỗi cơ sở dữ liệu khi từ chối sản phẩm");
                }
                break;

            case "LOGOUT": // Format: LOGOUT
                if (!this.clientId.equals("Guest")) {
                    userManager.logout(this.clientId);
                    System.out.println("[Server]: User " + this.clientId + " đã đăng xuất.");
                    this.clientId = "Guest";
                }
                if (currentRoom != null) currentRoom.removeSubscriber(this);
                break;

            default:
                out.println("UNKNOWN_COMMAND|Lệnh không hợp lệ.");
        }
    }
    public synchronized void sendMessage(String msg) {
        if (out != null) {
            out.println(msg);
            out.flush();
        }
    }
    private void cleanUp() {
        try {
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