package com.tboat;

import com.tboat.models.StatusOfAuction;
import com.tboat.service.AuctionTimerService;
import com.tboat.socket.ClientHandler;
import com.tboat.service.AuctionManager;
import com.tboat.dao.AuctionSessionDAO;
import com.tboat.models.AuctionSession;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(100);
    public static final ExecutorService broadcastExecutor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        int port = 8888;
        System.out.println("[System]: Đang khởi tạo danh sách phòng đấu giá...");
        initAuctionRooms();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new ClientHandler(clientSocket));
                System.out.println("[Network]: Chấp nhận kết nối từ: " + clientSocket.getInetAddress());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void initAuctionRooms() {
        try {
            AuctionSessionDAO sessionDAO = new AuctionSessionDAO();
            AuctionManager auctionManager = AuctionManager.getInstance();

            // Lấy danh sách từ DB
            List<AuctionSession> availableSessions = sessionDAO.getAvailableAuctions();

            if (availableSessions == null || availableSessions.isEmpty()) {
                System.err.println("[WARNING]: Không tìm thấy phiên đấu giá nào khả dụng trong Database!");
                return;
            }

            for (AuctionSession session : availableSessions) {
                int roomId = session.getId();
                double startPrice = session.getCurrentPrice();
                auctionManager.createRoom(roomId, startPrice);
                AuctionTimerService.getInstance().scheduleAuction(session);
                System.out.println("[Init]: Đã kích hoạt và ném vào Timer Phòng ID: " + roomId);
            }

            System.out.println("[System]: Khởi tạo thành công " + availableSessions.size() + " phòng đấu giá.");

        } catch (Exception e) {
            System.err.println("[ERROR]: Lỗi nghiêm trọng khi khởi tạo phòng đấu giá: " + e.getMessage());
            e.printStackTrace();
        }
    }
}