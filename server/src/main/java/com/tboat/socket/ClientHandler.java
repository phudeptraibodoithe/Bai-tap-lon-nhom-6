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

    // Khởi tạo các DAO để sử dụng
    private UserDAO userDAO = new UserDAO();
    private AuctionSessionDAO auctionDAO = new AuctionSessionDAO();
    private HistoryBidDAO historyDAO = new HistoryBidDAO();

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
            try {
                if (currentRoom != null) currentRoom.removeSubscriber(this);
                if (out != null) out.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleCommand(String input) {
        String[] parts = input.split("\\|");
        if (parts.length == 0) return;

        String action = parts[0].toUpperCase();

        switch (action) {
            case "LOGIN": // LOGIN|user|pass
                if (parts.length < 3) return;
                ResponseCode loginRes = userDAO.checkLogin(parts[1], parts[2]);
                if (loginRes == ResponseCode.SUCCESS) {
                    this.clientId = parts[1];
                    out.println("LOGIN_SUCCESS|" + clientId);
                } else {
                    out.println("LOGIN_FAILED|" + loginRes.name());
                }
                break;

            case "REGISTER": // REGISTER|user|pass|nickname
                if (parts.length < 4) return;
                ResponseCode regRes = userDAO.addUser(parts[1], parts[2], parts[3]);
                out.println("REGISTER_RESULT|" + regRes.name());
                break;

            case "LOGOUT":
                out.println("LOGOUT_SUCCESS|Hẹn gặp lại!");
                this.clientId = "Guest";
                if (currentRoom != null) currentRoom.removeSubscriber(this);
                break;

            case "PROFILE": // PROFILE (lấy info của chính mình)
                User user = userDAO.getUser(clientId);
                if (user != null) {
                    out.println("PROFILE_INFO|" + user.getAccountName() + "|" + user.getNickname() + "|" + user.getBalance());
                } else {
                    out.println("ERROR|Vui lòng đăng nhập trước.");
                }
                break;

            case "LIST_AVAILABLE": // Lấy danh sách các phiên đang mở
                List<AuctionSession> sessions = auctionDAO.getAvailableAuctions();
                String listData = sessions.stream()
                        .map(s -> s.getId() + ":" + s.getName() + ":" + s.getCurrentPrice())
                        .collect(Collectors.joining(";"));
                out.println("AVAILABLE_AUCTIONS|" + (listData.isEmpty() ? "NONE" : listData));
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

            case "JOIN": // JOIN|roomName
                if (parts.length < 2) return;
                if (currentRoom != null) currentRoom.removeSubscriber(this);
                currentRoom = AuctionManager.getInstance().getRoom(parts[1]);
                currentRoom.addSubscriber(this);
                out.println("JOIN_SUCCESS|Bạn đã vào phòng: " + parts[1]);
                break;

            case "BID": // BID|price
                // 1. KIỂM TRA ĐĂNG NHẬP TRƯỚC (Thêm vào đây)
                if (clientId.equals("Guest")) {
                    out.println("ERROR|Bạn cần đăng nhập để đấu giá.");
                    break;
                }
                if (currentRoom == null || parts.length < 2) {
                    out.println("ERROR|Hãy JOIN phòng trước.");
                    return;
                }
                try {
                    double price = Double.parseDouble(parts[1]);
                    if (currentRoom.placeBid(price, clientId)) {
                        // Cố gắng lấy ID từ tên phòng, nếu không phải số thì cần logic tìm ID khác
                        try {
                            int sId = Integer.parseInt(currentRoom.getRoomName());
                            // Gọi DAO để cập nhật database
                            historyDAO.updateBidLeader(sId, clientId, price, 0);
                        } catch (NumberFormatException e) {
                            // Nếu tên phòng không phải là ID số, bạn có thể in log để debug
                            System.out.println("Lưu ý: Tên phòng '" + currentRoom.getRoomName() + "' không phải là ID số.");
                        }

                        currentRoom.broadcast("NEW_BID|" + price + "|" + clientId);
                    } else {
                        out.println("BID_FAILED|Giá không hợp lệ.");
                    }
                } catch (Exception e) {
                    out.println("ERROR|Dữ liệu giá không đúng định dạng.");
                }
                break;

            case "HISTORY": // HISTORY|account (Xem lịch sử thắng của account)
                String targetAcc = (parts.length > 1) ? parts[1] : clientId;
                List<History> histories = historyDAO.getHistoryByAccount(targetAcc);
                String histData = histories.stream()
                        .map(h -> h.getAuctionSessionId() + ":" + h.getFinalPrice())
                        .collect(Collectors.joining(";"));
                out.println("HISTORY_RESULT|" + (histData.isEmpty() ? "NONE" : histData));
                break;

            case "GET_BID": // GET_BID|sessionId (Xem ai đang dẫn đầu phiên đó)
                if (parts.length < 2) return;
                String leader = historyDAO.getLeadBidder(Integer.parseInt(parts[1]));
                out.println("BID_LEADER|" + (leader != null ? leader : "Chưa có ai"));
                break;

            default:
                out.println("UNKNOWN_COMMAND|Lệnh không hợp lệ.");
        }
    }

    public void sendMessage(String msg) {
        if (out != null) out.println(msg);
    }
}