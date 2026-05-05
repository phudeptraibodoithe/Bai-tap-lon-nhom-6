package com.tboat.socket;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.HistoryBidDAO;
import com.tboat.dao.ParticipationDAO;
import com.tboat.dao.UserDAO;
import com.tboat.database.DatabaseConnection;
import com.tboat.models.*;
import com.tboat.service.*;
import com.tboat.utils.ResponseCode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private String clientId = "Guest";
    private AuctionRoom currentRoom;
    private UserManager userManager = UserManager.getInstance();
    private UserDAO userDAO = new UserDAO();
    private HistoryBidDAO historyDAO = new HistoryBidDAO();
    private AuctionSessionDAO auctionDAO = new AuctionSessionDAO();
    private SellerService sellerService=new SellerService();
    private ParticipationDAO participationDAO=new ParticipationDAO();
    private static final List<String> PUBLIC_ACTIONS = Arrays.asList(
            "LOGIN", "REGISTER", "LIST_AVAILABLE", "GET_PENDING_ITEMS", "APPROVE_ITEM", "REJECT_ITEM"
    );
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>)
                    (src, typeOfT, context) -> new JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>)
                    (json, typeOfT, context) -> LocalDateTime.parse(json.getAsString()))
            .create();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            this.out = new PrintWriter(socket.getOutputStream(), true);

            // Đã sửa: Gửi lời chào bằng chuẩn JSON thay vì chuỗi thô
            sendSystemMessage("SERVER_READY", "Chào mừng bạn đến với hệ thống đấu giá TBoat!", null);

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
        try {
            JsonObject jsonReq = JsonParser.parseString(input).getAsJsonObject();
            String action = jsonReq.get("action").getAsString().toUpperCase();

            if (!PUBLIC_ACTIONS.contains(action) && clientId.equals("Guest")) {
                sendResponse(new Response<>("ERROR", "Vui lòng đăng nhập", null));
                return;
            }

            switch (action) {
                case "LOGIN": handleLogin(input, gson); break;
                case "REGISTER": handleRegister(input, gson); break;
                case "LIST_AVAILABLE": handleListAvailable(); break;
                case "JOIN": handleJoin(input, gson); break;
                case "BID": handleBid(input, gson); break;
                case "POST_ITEM": handlePostItem(input, gson); break;
                case "GET_PENDING_ITEMS": handleGetPendingItems(); break;
                case "APPROVE_ITEM": handleApproveItem(input, gson); break;
                case "REJECT_ITEM": handleRejectItem(input, gson); break;
                case "PROFILE": handleProfile(); break;
                case "UPDATE_PROFILE": handleUpdateProfile(input); break;
                case "TRANSACTION": handleTransaction(input); break;
                case "CANCEL_AUCTION": handleCancelAuction(input); break;
                case "GET_HISTORY": handleGetHistory(input); break;
                case "GET_MY_AUCTIONS": handleGetMyAuctions(input); break;
                case "GET_SESSION_BIDS": handleGetSessionBids(input); break;
                case "LOGOUT":
                    userManager.logout(this.clientId);
                    this.clientId = "Guest";
                    if (currentRoom != null) currentRoom.removeSubscriber(this);
                    sendResponse(new Response<>("SUCCESS", "Đã đăng xuất", null));
                    break;
                default:
                    sendResponse(new Response<>("ERROR", "Lệnh không xác định", null));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(new Response<>("ERROR", "Lỗi xử lý yêu cầu", null));
        }
    }

    private void handleLogin(String input, Gson gson) {
        Type type = new TypeToken<Request<User>>(){}.getType();
        Request<User> req = gson.fromJson(input, type);
        User credentials = req.getPayload();

        ResponseCode res = userManager.login(credentials.getAccountName(), credentials.getPassword(), this);

        if (res == ResponseCode.SUCCESS) {
            this.clientId = credentials.getAccountName();
            User fullUser = userDAO.getUser(this.clientId);
            sendResponse(new Response<>("SUCCESS", "Đăng nhập thành công", fullUser));
        } else {
            sendResponse(new Response<>("FAILED", res.name(), null));
        }
    }

    private void handleListAvailable() {
        List<AuctionSession> sessions = auctionDAO.getAvailableAuctions();
        sendResponse(new Response<>("SUCCESS", "Danh sách phiên đấu giá", sessions));
    }

    private void handleBid(String input, Gson gson) {
        if (currentRoom == null) {
            sendResponse(new Response<>("ERROR", "Bạn chưa vào phòng", null));
            return;
        }

        Participation userRole = participationDAO.getRoleType(clientId, currentRoom.getSessionId());

        if (userRole != null && "SELLER".equals(userRole.getRoleType())) {
            sendResponse(new Response<>("FAILED", "Bạn không thể tự đặt giá cho sản phẩm của mình!", null));
            return;
        }

        JsonObject json = JsonParser.parseString(input).getAsJsonObject();
        double price = json.get("payload").getAsDouble();

        double userBalance = userDAO.getBalance(clientId);
        if (userBalance < price) {
            sendResponse(new Response<>("FAILED", "Số dư không đủ để đặt mức giá này", null));
            return;
        }

        boolean bidAccepted = currentRoom.placeBid(price, clientId);

        if (bidAccepted) {
            if (userRole == null) {
                Participation p = new Participation(clientId, currentRoom.getSessionId(), "BIDDER");
                participationDAO.addParticipation(p);
            }
            sendResponse(new Response<>("SUCCESS", "Bạn đang dẫn đầu", price));
            JsonObject bidData = new JsonObject();
            bidData.addProperty("newPrice", price);
            bidData.addProperty("newLeader", clientId);
            currentRoom.broadcast("NEW_BID", clientId + " vừa đặt giá mới", bidData);
        } else {
            sendResponse(new Response<>("FAILED", "Giá đặt phải cao hơn giá hiện tại", currentRoom.getCurrentPrice()));
        }
    }

    private void handleRegister(String input, Gson gson) {
        Type type = new TypeToken<Request<User>>(){}.getType();
        Request<User> req = gson.fromJson(input, type);
        User user = req.getPayload();

        ResponseCode res = userManager.register(user.getAccountName(), user.getPassword(), user.getNickname());
        sendResponse(new Response<>(res == ResponseCode.SUCCESS ? "SUCCESS" : "FAILED", res.name(), null));
    }

    private void handlePostItem(String input, Gson gson) {
        try {
            JsonObject json = JsonParser.parseString(input).getAsJsonObject();
            JsonObject payload = json.getAsJsonObject("payload");

            AuctionSession session = new AuctionSession();

            session.setName(payload.get("name").getAsString());
            session.setDescription(payload.get("description").getAsString());
            session.setType(payload.get("type").getAsString());
            session.setImageURL(payload.get("imageURL").getAsString());
            session.setCurrentPrice(payload.get("currentPrice").getAsDouble());
            session.setBidIncrease(payload.get("bidIncrease").getAsDouble());

            LocalDateTime startTime = LocalDateTime.parse(payload.get("startTime").getAsString());
            LocalDateTime endTime = LocalDateTime.parse(payload.get("endTime").getAsString());
            session.setStartTime(startTime);
            session.setEndTime(endTime);

            session.setSellerAccountName(this.clientId);
            session.setStatusOfAuction(StatusOfAuction.PENDING);

            // Lưu vào DB
            int id = auctionDAO.addAuctionSession(session);

            if (id > 0) {
                Participation p=new Participation(this.clientId,id,"SELLER");
                participationDAO.addParticipation(p);
                sendResponse(new Response<>("SUCCESS", "Đăng sản phẩm thành công, đang chờ duyệt", id));
            } else {
                sendResponse(new Response<>("ERROR", "Lỗi lưu dữ liệu vào Database", null));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(new Response<>("ERROR", "Dữ liệu gửi lên không hợp lệ!", null));
        }
    }

    private void handleJoin(String input, Gson gson) {
        try {
            JsonObject json = JsonParser.parseString(input).getAsJsonObject();
            int sessionId = json.get("payload").getAsInt();
            AuctionRoom targetRoom = AuctionManager.getInstance().getRoom(sessionId);

            AuctionSession session = auctionDAO.getAuctionById(sessionId);

            if (targetRoom == null) {
                if (session != null && session.getStatusOfAuction() != StatusOfAuction.ENDED && session.getStatusOfAuction() != StatusOfAuction.valueOf("CANCELED")) {
                    AuctionManager.getInstance().createRoom(sessionId, session.getCurrentPrice());
                    targetRoom = AuctionManager.getInstance().getRoom(sessionId);
                }
            }

            if (targetRoom != null && session != null) {
                if (currentRoom != null) {
                    currentRoom.removeSubscriber(this);
                }
                currentRoom = targetRoom;
                currentRoom.addSubscriber(this);

                JsonObject joinData = new JsonObject();
                joinData.addProperty("currentPrice", currentRoom.getCurrentPrice());
                boolean isSeller = session.getSellerAccountName().equals(this.clientId);
                joinData.addProperty("isSeller", isSeller);
                sendResponse(new Response<>("JOIN_SUCCESS", "Vào phòng thành công", joinData));
            } else {
                sendResponse(new Response<>("ERROR", "Phòng đấu giá không tồn tại hoặc đã đóng.", null));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(new Response<>("ERROR", "Lỗi xử lý hệ thống khi vào phòng", null));
        }
    }

    private void handleGetPendingItems() {
        try {
            List<AuctionSession> pendingItems = auctionDAO.getPendingAuctions();

            // Đảm bảo không bao giờ trả về null, trả về danh sách rỗng [] nếu không có bài
            if (pendingItems == null) {
                pendingItems = new ArrayList<>();
            }

            // Gửi response về, message phải khớp 100% với Client đang check
            sendResponse(new Response<>("SUCCESS", "Danh sách chờ duyệt", pendingItems));
            System.out.println("[Server]: Đã gửi danh sách chờ duyệt cho Admin.");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(new Response<>("ERROR", "Lỗi lấy danh sách: " + e.getMessage(), null));
        }
    }

    private void handleGetHistory(String input) {
        try {
            String accountName = this.clientId;
            if (accountName == null || accountName.equals("Guest")) {
                sendResponse(new Response<>("ERROR", "Bạn chưa đăng nhập!", null));
                return;
            }
            List<History> historyList = historyDAO.getHistoryByAccount(accountName);
            if (historyList == null) {
                historyList = new ArrayList<>();
            }
            sendResponse(new Response<>("SUCCESS", "Lấy lịch sử thành công", historyList));

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(new Response<>("ERROR", "Lỗi xử lý lấy lịch sử đấu giá: " + e.getMessage(), null));
        }
    }

    private void handleGetMyAuctions(String input) {
        try {
            String accountName = this.clientId;
            if (accountName == null || accountName.equals("Guest")) {
                sendResponse(new Response<>("ERROR", "Bạn chưa đăng nhập!", null));
                return;
            }
            List<AuctionSession> myList = auctionDAO.getAuctionsBySeller(accountName);
            if (myList == null) {
                myList = new ArrayList<>();
            }
            sendResponse(new Response<>("SUCCESS", "Lấy danh sách sản phẩm thành công", myList));

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(new Response<>("ERROR", "Lỗi xử lý lấy danh sách sản phẩm: " + e.getMessage(), null));
        }
    }

    private void handleGetSessionBids(String input) {
        try {
            JsonObject json = JsonParser.parseString(input).getAsJsonObject();
            int sessionId = json.get("payload").getAsInt();
            List<Bid> bidList = historyDAO.getBidsBySession(sessionId);
            if (bidList == null) {
                bidList = new ArrayList<>();
            }
            sendResponse(new Response<>("SUCCESS", "Lấy danh sách Bid thành công", bidList));
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(new Response<>("ERROR", "Lỗi xử lý lấy danh sách Bid: " + e.getMessage(), null));
        }
    }

    private void handleCancelAuction(String input) {
        try {
            JsonObject json = JsonParser.parseString(input).getAsJsonObject();
            int sessionId = json.get("payload").getAsInt();

            boolean success = sellerService.cancelAuction(this.clientId, sessionId);

            if (success) {
                AuctionManager.getInstance().removeRoom(sessionId);
                sendResponse(new Response<>("SUCCESS", "Đã hủy phiên đấu giá thành công", sessionId));
                sendSystemMessage("AUCTION_CANCELED", "Phiên " + sessionId + " đã bị người bán hủy", sessionId);
            } else {
                sendResponse(new Response<>("ERROR", "Không thể hủy (Bạn không phải chủ phiên hoặc đã có người đặt giá)", null));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(new Response<>("ERROR", "Dữ liệu yêu cầu hủy không hợp lệ", null));
        }
    }

    private void handleApproveItem(String input, Gson gson) {
        try {
            JsonObject json = JsonParser.parseString(input).getAsJsonObject();
            int sessionId = json.get("payload").getAsInt();
            AuctionSession session = auctionDAO.getAuctionById(sessionId);

            if (session != null) {
                AuctionTimerService.getInstance().scheduleAuction(session);
                sendResponse(new Response<>("SUCCESS", "Đã duyệt! Hệ thống sẽ tự động canh giờ.", sessionId));
                System.out.println("[Server]: Admin đã duyệt phiên ID: " + sessionId);
            } else {
                sendResponse(new Response<>("ERROR", "Không tìm thấy sản phẩm cần duyệt (Lỗi Database)", null));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(new Response<>("ERROR", "Lỗi xử lý duyệt sản phẩm: " + e.getMessage(), null));
        }
    }

    private void handleRejectItem(String input, Gson gson) {
        try {
            JsonObject json = JsonParser.parseString(input).getAsJsonObject();
            int sessionId = json.get("payload").getAsInt();

            boolean success = auctionDAO.updateSessionStatus(sessionId, StatusOfAuction.valueOf("CANCELED"));

            if (success) {
                sendResponse(new Response<>("SUCCESS", "Đã từ chối sản phẩm", sessionId));
                System.out.println("[Server]: Admin đã từ chối phiên ID: " + sessionId);
            } else {
                sendResponse(new Response<>("ERROR", "Không thể thực hiện từ chối (Lỗi Database)", null));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(new Response<>("ERROR", "Lỗi xử lý từ chối sản phẩm: " + e.getMessage(), null));
        }
    }

    private void handleProfile() {
        User fullUser = userDAO.getUser(this.clientId);
        if (fullUser != null) {
            // Gửi trả status PROFILE_INFO khớp với Frontend gốc
            sendResponse(new Response<>("PROFILE_INFO", "Thông tin hồ sơ", fullUser));
        } else {
            sendResponse(new Response<>("ERROR", "Không tìm thấy người dùng", null));
        }
    }

    private void handleUpdateProfile(String input) {
        JsonObject json = JsonParser.parseString(input).getAsJsonObject();
        JsonObject payload = json.getAsJsonObject("payload");

        String desc = payload.has("description") ? payload.get("description").getAsString() : "";

        // ĐÃ SỬA: Đổi "avatar" thành "avatarURL" cho khớp với Client gửi lên
        String avatar = payload.has("avatarURL") ? payload.get("avatarURL").getAsString() : "";

        // Gọi DB cập nhật
        boolean updateOk = userDAO.updateProfile(this.clientId, desc, avatar);

        if (updateOk) {
            sendResponse(new Response<>("SUCCESS", "Cập nhật thành công", null)); // Chú ý: Đổi lại status thành SUCCESS để khớp dòng 159 bên Client
        } else {
            sendResponse(new Response<>("ERROR", "Lỗi cập nhật", null));
        }
    }

    private void handleTransaction(String input) {
        JsonObject json = JsonParser.parseString(input).getAsJsonObject();
        double amount = json.get("payload").getAsDouble();
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean transOk = userDAO.updateBalance(conn, this.clientId, amount);
            if (transOk) {
                // ĐÃ SỬA: Đổi "TRANSACTION_SUCCESS" thành "SUCCESS" cho khớp với dòng 90 NapRutController
                sendResponse(new Response<>("SUCCESS", "Giao dịch đã được xử lý!", amount));
            } else {
                // ĐÃ SỬA: Đổi "TRANSACTION_FAILED" thành "FAILED" cho khớp với dòng 101 NapRutController
                sendResponse(new Response<>("FAILED", "Giao dịch bị từ chối (Số dư không đủ).", null));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendResponse(new Response<>("ERROR", "Lỗi kết nối cơ sở dữ liệu", null));
        }
    }

    public synchronized void sendResponse(Response<?> response) {
        String json = gson.toJson(response);
        if (out != null) {
            out.println(json);
            out.flush();
        }
    }

    public synchronized void sendSystemMessage(String action, String message, Object payload) {
        Response<Object> response = new Response<>(action, message, payload);
        String json = gson.toJson(response);
        if (out != null) {
            out.println(json);
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