package com.tboat.service;

import com.google.gson.JsonObject;
import com.tboat.dao.AuctionSessionDAO;
import com.tboat.models.auction.AuctionSession;
import com.tboat.models.auction.StatusOfAuction;
import com.tboat.models.network.ServerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

public class AuctionTimerService {

    private static final Logger log = LoggerFactory.getLogger(AuctionTimerService.class);
    private static final int TIMER_THREAD_POOL_SIZE = 4;
    private static final long IMMEDIATE_DELAY_SECONDS = 0L;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(TIMER_THREAD_POOL_SIZE);
    private final Map<Integer, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private static volatile AuctionTimerService instance;
    private final AuctionSessionDAO sessionDao = new AuctionSessionDAO();

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
        StatusOfAuction currentStatus = session.getStatusOfAuction();

        if (currentStatus == StatusOfAuction.PENDING ||
                currentStatus == StatusOfAuction.CANCELED ||
                currentStatus == StatusOfAuction.ENDED) {
            log.warn("[Timer] Phiên {} đang ở trạng thái {}, không đưa vào hệ thống tự động.", sessionId, currentStatus);
            return;
        }

        /*
         * Mỗi phiên đấu giá chỉ có một bộ hẹn giờ đang hoạt động. Hủy tác vụ cũ trước,
         * rồi thêm tác vụ bắt đầu hoặc kết thúc dựa trên thời điểm hiện tại.
         */
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = session.getStartTime();
        LocalDateTime endTime = session.getEndTime();

        cancelTask(sessionId);

        if (now.isAfter(endTime)) {
            log.info("[Timer] Phiên {} đã quá hạn, tiến hành ĐÓNG VÀ THÔNG BÁO.", sessionId);
            closeAuction(sessionId);
            return;
        }

        if (now.isBefore(startTime)) {
            long delayToStart = Duration.between(now, startTime).getSeconds();
            sessionDao.updateSessionStatus(sessionId, StatusOfAuction.NOT_STARTED);

            log.info("[Timer] Phiên {} sẽ tự động BẮT ĐẦU sau {} giây.", sessionId, delayToStart);

            ScheduledFuture<?> startFuture = scheduler.schedule(() -> {
                AuctionSession currentSession = sessionDao.getAuctionById(sessionId);
                if (currentSession != null && currentSession.getStatusOfAuction() == StatusOfAuction.NOT_STARTED) {

                    log.info("[Timer] ĐẾN GIỜ! Phiên {} bắt đầu nhận Bid.", sessionId);
                    sessionDao.updateSessionStatus(sessionId, StatusOfAuction.ONGOING);

                    AuctionRoom room = AuctionManager.getInstance().getRoom(sessionId);
                    if (room != null) {
                        room.broadcast(ServerEvent.AUCTION_STARTED,
                                "Phiên đấu giá đã chính thức bắt đầu!", null);
                    }

                    scheduleAuctionClose(sessionId, endTime);
                } else {
                    log.warn("[Timer] Bỏ qua chuyển ONGOING. Phiên {} đã bị đổi trạng thái hoặc không tồn tại.", sessionId);
                }
            }, delayToStart, TimeUnit.SECONDS);

            scheduledTasks.put(sessionId, startFuture);

        } else {
            sessionDao.updateSessionStatus(sessionId, StatusOfAuction.ONGOING);
            log.info("[Timer] Phiên {} được mở BẮT ĐẦU ngay lập tức.", sessionId);

            AuctionRoom room = AuctionManager.getInstance().getRoom(sessionId);
            if (room != null) {
                room.broadcast(ServerEvent.AUCTION_STARTED,
                        "Phiên đấu giá hiện đang diễn ra, bạn có thể đặt giá!", null);
            }

            scheduleAuctionClose(sessionId, endTime);
        }
    }

    public void scheduleAuctionClose(int sessionId, LocalDateTime endTime) {
        cancelTask(sessionId);
        long delay = Duration.between(LocalDateTime.now(), endTime).getSeconds();

        if (delay <= IMMEDIATE_DELAY_SECONDS) {
            closeAuction(sessionId);
            return;
        }

        log.info("[Timer] Phiên {} sẽ tự động CHỐT ĐƠN sau {} giây.", sessionId, delay);
        ScheduledFuture<?> future = scheduler.schedule(() -> closeAuction(sessionId), delay, TimeUnit.SECONDS);
        scheduledTasks.put(sessionId, future);
    }

    /**
     * Gia hạn một phiên đấu giá đang hoạt động và báo cho mọi máy khách trong phòng đặt lại timer.
     */
    public void extendAuction(int sessionId, int secondsToAdd) {
        AuctionSession session = sessionDao.getAuctionById(sessionId);
        if (session == null) {
            log.warn("[Timer] Không tìm thấy phiên {} để gia hạn.", sessionId);
            return;
        }

        LocalDateTime currentEnd = session.getEndTime();
        LocalDateTime newEndTime = (currentEnd.isBefore(LocalDateTime.now()) ?
                LocalDateTime.now() : currentEnd).plusSeconds(secondsToAdd);

        sessionDao.updateEndTime(sessionId, newEndTime);
        scheduleAuctionClose(sessionId, newEndTime);

        AuctionRoom room = AuctionManager.getInstance().getRoom(sessionId);
        if (room != null) {
            JsonObject payload = new JsonObject();
            payload.addProperty("newEndTime", newEndTime.toString());
            payload.addProperty("secondsAdded", secondsToAdd);
            room.broadcast(ServerEvent.TIME_EXTENDED,
                    "Phiên đấu giá được gia hạn thêm " + secondsToAdd + " giây", payload);
            log.info("[Timer] Đã broadcast TIME_EXTENDED cho phiên {}, endTime mới: {}", sessionId, newEndTime);
        } else {
            log.warn("[Timer] Phiên {} không có room active, không thể broadcast TIME_EXTENDED.", sessionId);
        }
    }

    private void cancelTask(int sessionId) {
        ScheduledFuture<?> task = scheduledTasks.remove(sessionId);
        if (task != null) task.cancel(false);
    }

    private void closeAuction(int sessionId) {
        scheduledTasks.remove(sessionId);
        AuctionRoom room = AuctionManager.getInstance().getRoom(sessionId);
        if (room != null) {
            room.finishAuction();
        } else {
            sessionDao.updateSessionStatus(sessionId, StatusOfAuction.ENDED);
            log.info("[Timer] Phiên {} đã kết thúc (Không có người chơi trong phòng).", sessionId);
        }
    }
}
