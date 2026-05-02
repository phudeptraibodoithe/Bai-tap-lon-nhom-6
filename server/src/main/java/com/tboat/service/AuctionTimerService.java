package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.models.AuctionSession;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

public class AuctionTimerService {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    // Dùng int cho ID task
    private final Map<Integer, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private static volatile AuctionTimerService instance;

    private AuctionTimerService() {}

    public static AuctionTimerService getInstance() {
        if (instance == null) {
            synchronized (AuctionTimerService.class) {
                if (instance == null) instance = new AuctionTimerService();
            }
        }
        return instance;
    }

    public void scheduleAuctionClose(int sessionId, LocalDateTime endTime) {
        cancelTask(sessionId);

        long delay = Duration.between(LocalDateTime.now(), endTime).getSeconds();

        if (delay <= 0) {
            closeAuction(sessionId);
            return;
        }

        ScheduledFuture<?> future = scheduler.schedule(() -> closeAuction(sessionId), delay, TimeUnit.SECONDS);
        scheduledTasks.put(sessionId, future);
    }

    public void extendAuction(int sessionId, int secondsToAdd) {
        AuctionSessionDAO dao = new AuctionSessionDAO();
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
            room.finishAuction();
        }
    }
}