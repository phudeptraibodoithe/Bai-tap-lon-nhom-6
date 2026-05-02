package com.tboat.socket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.HistoryBidDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.*;
import com.tboat.service.AuctionManager;
import com.tboat.service.AuctionRoom;
import com.tboat.service.AuctionTimerService;
import com.tboat.service.UserManager;
import com.tboat.utils.ResponseCode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

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
    private static final Gson gson = new Gson();

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
        //Gson gson = new Gson();
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
        JsonObject json = JsonParser.parseString(input).getAsJsonObject();
        double price = json.get("payload").getAsDouble();

        // --- BƯỚC CẢI TIẾN: Kiểm tra số dư người dùng ---
        User currentUser = userDAO.getUser(clientId);
        if (currentUser == null || currentUser.getBalance() < price) {
            sendResponse(new Response<>("FAILED", "Số dư không đủ để đặt mức giá này", null));
            return;
        }
        // -------------------------------------------------

        // Thực hiện đặt giá
        boolean bidAccepted = currentRoom.placeBid(price, clientId);

        if (bidAccepted) {
            // Phản hồi riêng cho người vừa bid thành công
            sendResponse(new Response<>("SUCCESS", "Bạn đang dẫn đầu", price));

            // Gửi thông báo cho TẤT CẢ mọi người trong phòng (dùng 3 tham số mới)
            currentRoom.broadcast("NEW_BID", clientId + " vừa đặt giá mới", price);
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
        Type type = new TypeToken<Request<AuctionSession>>(){}.getType();
        Request<AuctionSession> req = gson.fromJson(input, type);
        AuctionSession session = req.getPayload();

        session.setSellerAccountName(this.clientId);
        session.setStatusOfAuction(StatusOfAuction.PENDING);
        session.setStartTime(java.time.LocalDateTime.now());

        int id = auctionDAO.addAuctionSession(session);
        if (id > 0) {
            AuctionManager.getInstance().createRoom(String.valueOf(id), session.getCurrentPrice());
            sendResponse(new Response<>("SUCCESS", "Đăng sản phẩm thành công, đang chờ duyệt", id));
        } else {
            sendResponse(new Response<>("ERROR", "Lỗi lưu dữ liệu", null));
        }
    }

    private void handleJoin(String input, Gson gson) {
        JsonObject json = JsonParser.parseString(input).getAsJsonObject();
        String roomName = json.get("payload").getAsString();
        AuctionRoom targetRoom = AuctionManager.getInstance().getRoom(roomName);

        if (targetRoom != null) {
            if (currentRoom != null) currentRoom.removeSubscriber(this);
            currentRoom = targetRoom;
            currentRoom.addSubscriber(this);
            sendResponse(new Response<>("JOIN_SUCCESS", roomName, currentRoom.getCurrentPrice()));
        } else {
            sendResponse(new Response<>("ERROR", "Phòng không tồn tại", null));
        }
    }

    private void handleGetPendingItems() {
        List<AuctionSession> pendingItems = auctionDAO.getPendingAuctions();
        sendResponse(new Response<>("SUCCESS", "Danh sách chờ duyệt", pendingItems));
    }

    private void handleApproveItem(String input, Gson gson) {
        JsonObject json = JsonParser.parseString(input).getAsJsonObject();
        int sessionId = json.get("payload").getAsInt();

        boolean success = auctionDAO.updateSessionStatus(sessionId, StatusOfAuction.valueOf("OPENING"));

        if (success) {
            // --- BỔ SUNG: Lấy thông tin session để bắt đầu đếm ngược ---
            AuctionSession session = auctionDAO.getAuctionById(sessionId);
            if (session != null) {
                AuctionTimerService.getInstance().scheduleAuctionClose(sessionId, session.getEndTime());
            }
            sendResponse(new Response<>("SUCCESS", "Đã duyệt và bắt đầu đấu giá", sessionId));
        } else {
            sendResponse(new Response<>("ERROR", "Không thể duyệt sản phẩm", null));
        }
    }

    private void handleRejectItem(String input, Gson gson) {
        JsonObject json = JsonParser.parseString(input).getAsJsonObject();
        int sessionId = json.get("payload").getAsInt();

        boolean success = auctionDAO.updateSessionStatus(sessionId, StatusOfAuction.valueOf("CANCELED"));

        if (success) {
            sendResponse(new Response<>("SUCCESS", "Đã từ chối sản phẩm", sessionId));
        } else {
            sendResponse(new Response<>("ERROR", "Không thể thực hiện", null));
        }
    }

    public synchronized void sendResponse(Response<?> response) {
        String json = gson.toJson(response);
        out.println(json);
        out.flush();
    }
    // Thêm vào ClientHandler
    public synchronized void sendSystemMessage(String action, String message, Object payload) {
        //Gson gson = new Gson();
        // Đóng gói vào Object Response giống hệt các xử lý LOGIN, REGISTER...
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