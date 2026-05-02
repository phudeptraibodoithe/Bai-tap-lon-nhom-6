package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.database.DatabaseConnection;
import com.tboat.models.AuctionSession;
import com.tboat.models.StatusOfAuction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

public class AuctionTimerService {
    // Dùng chung một Scheduler cho toàn Server để tiết kiệm tài nguyên
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    // Lưu trữ các task đang chờ để có thể hủy/gia hạn
    private final Map<Integer, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    private static AuctionTimerService instance;
    private AuctionTimerService() {}
    public static synchronized AuctionTimerService getInstance() {
        if (instance == null) instance = new AuctionTimerService();
        return instance;
    }

    /**
     * Lên lịch đóng phiên đấu giá
     */
    public void scheduleAuctionClose(int sessionId, LocalDateTime endTime) {
        // Nếu đã có task cũ cho phiên này (do gia hạn), hãy hủy nó trước
        cancelTask(sessionId);

        long delay = Duration.between(LocalDateTime.now(), endTime).getSeconds();

        if (delay <= 0) {
            closeAuction(sessionId);
            return;
        }

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            closeAuction(sessionId);
        }, delay, TimeUnit.SECONDS);

        scheduledTasks.put(sessionId, future);
    }

    /**
     * Tính năng Sniper Protection: Gia hạn thời gian
     */
    public void extendAuction(int sessionId, int secondsToAdd) {
        AuctionSessionDAO dao = new AuctionSessionDAO();
        AuctionSession session = dao.getAuctionById(sessionId);

        if (session != null) {
            // Gia hạn từ endTime hiện tại hoặc từ NOW nếu đã quá hạn
            LocalDateTime currentEnd = session.getEndTime();
            LocalDateTime newEndTime = (currentEnd.isBefore(LocalDateTime.now()) ?
                    LocalDateTime.now() : currentEnd).plusSeconds(secondsToAdd);

            // 1. Cập nhật vào DB (BẮT BUỘC)
            dao.updateEndTime(sessionId, newEndTime);

            // 2. Lập lịch lại
            scheduleAuctionClose(sessionId, newEndTime);
        }
    }

    private void cancelTask(int sessionId) {
        ScheduledFuture<?> future = scheduledTasks.remove(sessionId);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void closeAuction(int sessionId) {
        scheduledTasks.remove(sessionId);
        AuctionSessionDAO dao = new AuctionSessionDAO();
        AuctionSession session = dao.getAuctionById(sessionId);

        if (session != null) {
            // 1. Cập nhật Status thành FINISHED trong DB
            dao.updateSessionStatus(sessionId, StatusOfAuction.ENDED);

            // 2. Thông báo cho tất cả user trong phòng đó
            AuctionRoom room = AuctionManager.getInstance().getRoom(String.valueOf(sessionId));
            if (room != null) {
                String winner = session.getHighestBidderAccount();
                String msg = (winner == null) ? "Phiên kết thúc - Không có người mua." :
                        "Phiên kết thúc! Người thắng: " + winner;
                room.broadcast("AUCTION_FINISHED", msg, session.getCurrentPrice());

                // 3. Giải phóng phòng khỏi bộ nhớ sau khi kết thúc
                AuctionManager.getInstance().removeRoom(String.valueOf(sessionId));
            }
        }
    }
}