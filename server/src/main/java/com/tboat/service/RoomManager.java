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

public class RoomManager {
    private static RoomManager instance;
    private final Map<Integer, AuctionRoom> activeRooms = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AuctionSessionDAO sessionDAO = new AuctionSessionDAO();

    public static synchronized RoomManager getInstance() {
        if (instance == null) instance = new RoomManager();
        return instance;
    }

    private RoomManager() {
        scheduler.scheduleAtFixedRate(this::autoStartAuctions, 0, 10, TimeUnit.SECONDS);
    }

    private void autoStartAuctions() {
        List<AuctionSession> pendings = sessionDAO.getPendingAuctions();
        LocalDateTime now = LocalDateTime.now();
        for (AuctionSession s : pendings) {
            if (s.getStartTime().isBefore(now)) {
                System.out.println("[RoomManager]: Kích hoạt phiên " + s.getId());
                sessionDAO.updateSessionStatus(s.getId(), StatusOfAuction.ONGOING);
                createRoom(s.getId(), s.getCurrentPrice());
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
    }
}