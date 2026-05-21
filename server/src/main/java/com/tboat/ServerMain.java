package com.tboat;

import com.tboat.logging.LogConfig;
import com.tboat.service.AuctionTimerService;
import com.tboat.socket.ClientConnection;
import com.tboat.service.AuctionManager;
import com.tboat.dao.AuctionSessionDAO;
import com.tboat.models.auction.AuctionSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(30);
    public static final ExecutorService broadcastExecutor = Executors.newFixedThreadPool(10);
    private static final Logger logger = LoggerFactory.getLogger(ServerMain.class);

    public static void main(String[] args) {
        LogConfig logConfig = new LogConfig("logs/server", 10);
        logConfig.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Server đang tắt...");
            logConfig.stop();
        }));

        int port = 8888;
        logger.info("[System]: Đang khởi tạo danh sách phòng đấu giá...");
        initAuctionRooms();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new ClientConnection(clientSocket));
                logger.info("[Network]: Chấp nhận kết nối từ: {}", clientSocket.getInetAddress());
            }
        } catch (IOException e) {
            logger.error("[Network]: Lỗi khởi động ServerSocket!", e);
        }
    }

    private static void initAuctionRooms() {
        try {
            AuctionSessionDAO sessionDAO = new AuctionSessionDAO();
            AuctionManager auctionManager = AuctionManager.getInstance();

            // Lấy danh sách từ DB
            List<AuctionSession> availableSessions = sessionDAO.getAvailableAuctions();

            if (availableSessions == null || availableSessions.isEmpty()) {
                logger.warn("[WARNING]: Không tìm thấy phiên đấu giá nào khả dụng trong Database!");
                return;
            }

            for (AuctionSession session : availableSessions) {
                int roomId = session.getId();
                double startPrice = session.getCurrentPrice();
                auctionManager.createRoom(roomId, startPrice);
                AuctionTimerService.getInstance().scheduleAuction(session);
                logger.info("[Init]: Đã kích hoạt Phòng ID: {}", roomId);
            }

            logger.info("[System]: Khởi tạo thành công {} phòng đấu giá.", availableSessions.size());

        } catch (Exception e) {
            logger.error("[ERROR]: Lỗi nghiêm trọng khi khởi tạo phòng đấu giá: {}", e.getMessage(), e);
        }
    }
}