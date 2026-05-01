package com.tboat;

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
    // Tạo một hồ chứa luồng (Pool) tối đa 100 người chơi cùng lúc
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(100);

    public static void main(String[] args) {
        int port = 8888;

        // --- BƯỚC CẢI TIẾN: Khởi tạo dữ liệu hệ thống ---
        System.out.println("[System]: Đang khởi tạo danh sách phòng đấu giá...");
        initAuctionRooms();
        // ----------------------------------------------

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                // Thay vì tạo thread mới thủ công, hãy ném vào Pool
                threadPool.execute(new ClientHandler(clientSocket));
                System.out.println("[Network]: Chấp nhận kết nối từ: " + clientSocket.getInetAddress());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * CẢI TIẾN: Lấy các phiên đấu giá đang active từ DB và tạo phòng.
     * Lý do sửa: Thêm xử lý ngoại lệ và log chi tiết để kiểm soát luồng dữ liệu.
     */
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
                // Chuyển ID thành String để làm key cho Map trong AuctionManager
                String roomId = String.valueOf(session.getId());
                double startPrice = session.getCurrentPrice();

                // Tạo phòng vật lý trong bộ nhớ Server
                auctionManager.createRoom(roomId, startPrice);

                System.out.println("[Init]: Đã kích hoạt Phòng ID: " + roomId + " | Giá hiện tại: " + startPrice);
            }

            System.out.println("[System]: Khởi tạo thành công " + availableSessions.size() + " phòng đấu giá.");

        } catch (Exception e) {
            // LỢI ÍCH: Nếu lỗi DB (sai pass, mất mạng), Server vẫn báo lỗi rõ ràng thay vì im lặng
            System.err.println("[ERROR]: Lỗi nghiêm trọng khi khởi tạo phòng đấu giá: " + e.getMessage());
            e.printStackTrace();
        }
    }
}