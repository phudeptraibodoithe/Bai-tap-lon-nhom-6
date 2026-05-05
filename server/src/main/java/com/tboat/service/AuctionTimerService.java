package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.models.AuctionSession;
import com.tboat.models.StatusOfAuction;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

public class AuctionTimerService {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final Map<Integer, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private static volatile AuctionTimerService instance;
    private final AuctionSessionDAO dao = new AuctionSessionDAO();

    private AuctionTimerService() {}

    public static AuctionTimerService getInstance() {
        if (instance == null) {
            synchronized (AuctionTimerService.class) {
                if (instance == null) instance = new AuctionTimerService();
            }
        }
        return instance;
    }

    public void scheduleAuction(AuctionSession session) {
        int sessionId = session.getId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = session.getStartTime();
        LocalDateTime endTime = session.getEndTime();

        cancelTask(sessionId);

        // ==========================================
        // TRƯỜNG HỢP 1: Đã quá hạn kết thúc
        // ==========================================
        if (now.isAfter(endTime)) {
            System.out.println("[Timer]: Phiên " + sessionId + " đã quá hạn, tiến hành ĐÓNG VÀ THÔNG BÁO.");
            // SỬA Ở ĐÂY: Gọi hàm closeAuction để nó gửi thông báo AUCTION_FINISHED cho Client
            closeAuction(sessionId);
            return;
        }

        // ==========================================
        // TRƯỜNG HỢP 2: Chưa tới giờ -> Hẹn giờ mở cửa
        // ==========================================
        if (now.isBefore(startTime)) {
            long delayToStart = Duration.between(now, startTime).getSeconds();
            dao.updateSessionStatus(sessionId, StatusOfAuction.NOT_STARTED);

            System.out.println("[Timer]: Phiên " + sessionId + " sẽ tự động BẮT ĐẦU sau " + delayToStart + " giây.");

            ScheduledFuture<?> startFuture = scheduler.schedule(() -> {
                System.out.println("[Timer]: ĐẾN GIỜ! Phiên " + sessionId + " bắt đầu nhận Bid.");
                dao.updateSessionStatus(sessionId, StatusOfAuction.ONGOING);

                // Gửi thông báo cho mọi người trong phòng
                AuctionRoom room = AuctionManager.getInstance().getRoom(sessionId);
                if (room != null) {
                    room.broadcast("AUCTION_STARTED", "Phiên đấu giá đã chính thức bắt đầu!", null);
                }

                scheduleAuctionClose(sessionId, endTime);
            }, delayToStart, TimeUnit.SECONDS);

            scheduledTasks.put(sessionId, startFuture);
        }
        // ==========================================
        // TRƯỜNG HỢP 3: Đang diễn ra -> Mở luôn
        // ==========================================
        else {
            dao.updateSessionStatus(sessionId, StatusOfAuction.ONGOING);
            System.out.println("[Timer]: Phiên " + sessionId + " được mở BẮT ĐẦU ngay lập tức.");

            AuctionRoom room = AuctionManager.getInstance().getRoom(sessionId);
            if (room != null) {
                room.broadcast("AUCTION_STARTED", "Phiên đấu giá hiện đang diễn ra, bạn có thể đặt giá!", null);
            }

            scheduleAuctionClose(sessionId, endTime);
        }
    }

    public void scheduleAuctionClose(int sessionId, LocalDateTime endTime) {
        cancelTask(sessionId);
        long delay = Duration.between(LocalDateTime.now(), endTime).getSeconds();

        if (delay <= 0) {
            closeAuction(sessionId); // Hàm này chứa lệnh gọi room.finishAuction() (có gửi broadcast)
            return;
        }

        System.out.println("[Timer]: Phiên " + sessionId + " sẽ tự động CHỐT ĐƠN sau " + delay + " giây.");
        ScheduledFuture<?> future = scheduler.schedule(() -> closeAuction(sessionId), delay, TimeUnit.SECONDS);
        scheduledTasks.put(sessionId, future);
    }

    public void extendAuction(int sessionId, int secondsToAdd) {
        AuctionSession session = dao.getAuctionById(sessionId);
        if (session != null) {
            LocalDateTime currentEnd = session.getEndTime();
            LocalDateTime newEndTime = (currentEnd.isBefore(LocalDateTime.now()) ?
                    LocalDateTime.now() : currentEnd).plusSeconds(secondsToAdd);

            dao.updateEndTime(sessionId, newEndTime);
            scheduleAuctionClose(sessionId, newEndTime);
        }
    }

    private void cancelTask(int sessionId) {
        ScheduledFuture<?> future = scheduledTasks.remove(sessionId);
        if (future != null) future.cancel(false);
    }

    private void closeAuction(int sessionId) {
        scheduledTasks.remove(sessionId);
        AuctionRoom room = AuctionManager.getInstance().getRoom(sessionId);
        if (room != null) {
            // LỆNH NÀY CHÍNH LÀ LÚC SERVER GỬI THÔNG BÁO KẾT THÚC CHO CLIENT
            room.finishAuction();
        } else {
            // Nếu phòng không có ai thì chỉ cần update Database
            dao.updateSessionStatus(sessionId, StatusOfAuction.ENDED);
            System.out.println("[Timer]: Phiên " + sessionId + " đã kết thúc (Không có người chơi trong phòng).");
        }
    }
}