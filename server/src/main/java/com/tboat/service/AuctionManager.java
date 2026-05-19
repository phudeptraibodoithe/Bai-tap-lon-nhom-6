package com.tboat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {
    private static final Logger logger = LoggerFactory.getLogger(AuctionManager.class);
    private static volatile AuctionManager instance;
    private final Map<Integer, AuctionRoom> activeRooms = new ConcurrentHashMap<>();

    private AuctionManager() {}

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

    public void createRoom(int sessionId, double initialPrice) {
        activeRooms.putIfAbsent(sessionId, new AuctionRoom(sessionId, initialPrice));
    }

    public AuctionRoom getRoom(int sessionId) {
        return activeRooms.get(sessionId);
    }

    public void removeRoom(int sessionId) {
        activeRooms.remove(sessionId);
        logger.info("[AuctionManager]: Đã giải phóng phòng {} khỏi bộ nhớ.", sessionId);
    }
}