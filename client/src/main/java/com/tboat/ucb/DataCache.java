package com.tboat.ucb;

import com.tboat.models.network.ServerEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Lưu cache phản hồi từ server theo từng action.
 * An toàn luồng nhờ dùng ConcurrentHashMap.
 *
 * Thời gian sống theo từng loại action:
 *  - Dữ liệu đấu giá (giá, bid): ngắn khoảng 15-30 giây vì thay đổi liên tục
 *  - Danh sách chờ duyệt: khoảng 30 giây
 *  - Hồ sơ/Số dư: khoảng 2 phút
 */
public class DataCache {

    private static final Logger log = Logger.getLogger(DataCache.class.getName());
    private static DataCache instance;
    private static final long AUCTION_LIST_TTL_MS = 30_000L;
    private static final long PROFILE_TTL_MS = 120_000L;
    private static final long AVAILABLE_LIST_TTL_MS = 20_000L;
    private static final long AUCTION_HISTORY_TTL_MS = 60_000L;
    private static final long SESSION_BIDS_TTL_MS = 15_000L;
    private static final long DEFAULT_TTL_FALLBACK = 30_000L;

    // Bản đồ action sang mục cache
    private final Map<ServerEvent, CacheEntry> cache = new ConcurrentHashMap<>();

    // ── TTL mặc định theo action (mili giây) ────────────────────────────────
    public static final Map<ServerEvent, Long> DEFAULT_TTL = Map.of(
            ServerEvent.GET_ALL_ITEMS,      AUCTION_LIST_TTL_MS,
            ServerEvent.GET_PROFILE,        PROFILE_TTL_MS,
            ServerEvent.LIST_AVAILABLE,     AVAILABLE_LIST_TTL_MS,
            ServerEvent.GET_MY_AUCTIONS,    AUCTION_LIST_TTL_MS,
            ServerEvent.GET_HISTORY,        AUCTION_HISTORY_TTL_MS,
            ServerEvent.GET_SESSION_BIDS,   SESSION_BIDS_TTL_MS
    );

    private DataCache() {}

    public static synchronized DataCache getInstance() {
        if (instance == null) instance = new DataCache();
        return instance;
    }

    // ─── Lưu phản hồi vào cache ─────────────────────────────────────────────
    public void put(ServerEvent action, String jsonResponse) {
        cache.put(action, new CacheEntry(jsonResponse, System.currentTimeMillis()));
        log.fine("[Cache] Saved: " + action.name());
    }

    // ─── Lấy cache nếu còn mới ──────────────────────────────────────────────
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

    // ─── Lớp nội bộ ─────────────────────────────────────────────────────────
    private static class CacheEntry {
        final String json;
        final long timestamp;

        CacheEntry(String json, long timestamp) {
            this.json = json;
            this.timestamp = timestamp;
        }
    }
}
