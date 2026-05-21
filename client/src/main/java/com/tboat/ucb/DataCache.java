package com.tboat.ucb;

import com.tboat.models.network.ServerEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Lưu cache response từ server theo từng action.
 * Thread-safe dùng ConcurrentHashMap.
 *
 * TTL (Time-To-Live) theo từng loại action:
 *  - Dữ liệu đấu giá (giá, bid): TTL ngắn ~15-30s (thay đổi liên tục)
 *  - Danh sách chờ duyệt: TTL ~30s
 *  - Profile/Balance: TTL ~2 phút
 */
public class DataCache {

    private static final Logger log = Logger.getLogger(DataCache.class.getName());
    private static DataCache instance;

    // Map<action, CacheEntry>
    private final Map<ServerEvent, CacheEntry> cache = new ConcurrentHashMap<>();

    // ── TTL mặc định theo action (milliseconds) ──────────────────────────────
    public static final Map<ServerEvent, Long> DEFAULT_TTL = Map.of(
            ServerEvent.GET_ALL_ITEMS,      30_000L,
            ServerEvent.GET_PROFILE,       120_000L,
            ServerEvent.LIST_AVAILABLE,     20_000L,
            ServerEvent.GET_MY_AUCTIONS,    30_000L,
            ServerEvent.GET_HISTORY,        60_000L,
            ServerEvent.GET_SESSION_BIDS,   15_000L
    );
    private static final long DEFAULT_TTL_FALLBACK = 30_000L;

    private DataCache() {}

    public static synchronized DataCache getInstance() {
        if (instance == null) instance = new DataCache();
        return instance;
    }

    // ─── Lưu response vào cache ───────────────────────────────────────────────
    public void put(ServerEvent action, String jsonResponse) {
        cache.put(action, new CacheEntry(jsonResponse, System.currentTimeMillis()));
        log.fine("[Cache] Saved: " + action.name());
    }

    // ─── Lấy cache nếu còn fresh ─────────────────────────────────────────────
    public String get(ServerEvent action) {
        long ttl = DEFAULT_TTL.getOrDefault(action, DEFAULT_TTL_FALLBACK);
        CacheEntry entry = cache.get(action);
        if (entry == null) return null;
        if (System.currentTimeMillis() - entry.timestamp > ttl) {
            cache.remove(action);
            log.fine("[Cache] Expired: " + action.name());
            return null;
        }
        log.fine("[Cache] HIT: " + action.name());
        return entry.json;
    }

    // ─── Kiểm tra nhanh ──────────────────────────────────────────────────────
    public boolean isFresh(ServerEvent action) {
        return get(action) != null;
    }

    // ─── Xóa cache (dùng sau khi có thay đổi quan trọng) ────────────────────
    public void invalidate(ServerEvent action) {
        cache.remove(action);
        log.info("[Cache] Invalidated: " + action.name());
    }

    public void invalidateAll() {
        cache.clear();
        log.info("[Cache] Cleared all.");
    }

    // ─── Inner class ─────────────────────────────────────────────────────────
    private static class CacheEntry {
        final String json;
        final long timestamp;

        CacheEntry(String json, long timestamp) {
            this.json = json;
            this.timestamp = timestamp;
        }
    }
}
