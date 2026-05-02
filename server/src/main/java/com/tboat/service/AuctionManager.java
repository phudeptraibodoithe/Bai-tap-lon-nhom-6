package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.models.AuctionSession;
import com.tboat.models.StatusOfAuction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionManager {
    private static volatile AuctionManager instance;

    // Dùng int cho ID phòng
    private final Map<Integer, AuctionRoom> activeRooms = new ConcurrentHashMap<>();
    private final ScheduledExecutorService autoStartScheduler = Executors.newSingleThreadScheduledExecutor();
    private final AuctionSessionDAO sessionDAO = new AuctionSessionDAO();

    private AuctionManager() {
        autoStartScheduler.scheduleAtFixedRate(this::autoStartAuctions, 0, 10, TimeUnit.SECONDS);
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }

    private void autoStartAuctions() {
        List<AuctionSession> pendings = sessionDAO.getPendingAuctions();
        LocalDateTime now = LocalDateTime.now();
        for (AuctionSession s : pendings) {
            if (s.getStartTime().isBefore(now)) {
                System.out.println("[AuctionManager]: Kích hoạt phiên " + s.getId());
                sessionDAO.updateSessionStatus(s.getId(), StatusOfAuction.ONGOING);

                // Truyền trực tiếp ID kiểu int
                createRoom(s.getId(), s.getCurrentPrice());
                AuctionTimerService.getInstance().scheduleAuctionClose(s.getId(), s.getEndTime());
            }
        }
    }

    public void createRoom(int sessionId, double initialPrice) {
        activeRooms.putIfAbsent(sessionId, new AuctionRoom(sessionId, initialPrice));
    }

    public AuctionRoom getRoom(int sessionId) {
        return activeRooms.get(sessionId);
    }

    public void removeRoom(int sessionId) {
        activeRooms.remove(sessionId);
        System.out.println("[AuctionManager]: Đã giải phóng phòng " + sessionId + " khỏi bộ nhớ.");
    }
}