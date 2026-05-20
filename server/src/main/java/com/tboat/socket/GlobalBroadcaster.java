package com.tboat.socket;

import com.tboat.models.network.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GlobalBroadcaster — Singleton quản lý tất cả ClientContext đang kết nối.
 *
 * Dùng để:
 *  - Gửi lệnh reload cho admin/manager khi có sản phẩm mới, bid mới, v.v.
 *  - Gửi lệnh reload trang chủ (LIST_AVAILABLE) cho tất cả user khi có phiên mới được duyệt.
 *
 * Cách dùng:
 *  - Khi client kết nối:    GlobalBroadcaster.getInstance().register(context);
 *  - Khi client ngắt kết nối: GlobalBroadcaster.getInstance().unregister(context);
 */
public class GlobalBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(GlobalBroadcaster.class);

    private static volatile GlobalBroadcaster instance;

    /** Toàn bộ client đang kết nối */
    private final Set<ClientContext> allClients   = ConcurrentHashMap.newKeySet();

    /** Chỉ các client có role ADMIN hoặc MANAGER */
    private final Set<ClientContext> adminClients = ConcurrentHashMap.newKeySet();

    private GlobalBroadcaster() {}

    public static GlobalBroadcaster getInstance() {
        if (instance == null) {
            synchronized (GlobalBroadcaster.class) {
                if (instance == null) instance = new GlobalBroadcaster();
            }
        }
        return instance;
    }

    // ── Registry ─────────────────────────────────────────────────────────────

    /** Gọi khi client kết nối (trong SocketServer hoặc ClientContext init). */
    public void register(ClientContext ctx) {
        allClients.add(ctx);
        log.debug("[GlobalBroadcaster] Registered client: {}", ctx.getClientId());
    }

    /** Gọi khi client ngắt kết nối. */
    public void unregister(ClientContext ctx) {
        allClients.remove(ctx);
        adminClients.remove(ctx);
        log.debug("[GlobalBroadcaster] Unregistered client: {}", ctx.getClientId());
    }

    /**
     * Gọi sau khi xác định role của client (login xong).
     * role: "ADMIN" hoặc "MANAGER"
     */
    public void registerAdmin(ClientContext ctx) {
        adminClients.add(ctx);
        log.debug("[GlobalBroadcaster] Registered admin/manager: {}", ctx.getClientId());
    }

    // ── Broadcast ────────────────────────────────────────────────────────────

    /**
     * Gửi tín hiệu đến TẤT CẢ client đang kết nối.
     * Dùng cho: RELOAD_AVAILABLE (trang chủ cần cập nhật danh sách phiên).
     */
    public void broadcastToAll(Response<?> response) {
        int count = 0;
        for (ClientContext ctx : allClients) {
            try {
                ctx.sendResponse(response);
                count++;
            } catch (Exception e) {
                log.warn("[GlobalBroadcaster] Không thể gửi đến client {}: {}", ctx.getClientId(), e.getMessage());
            }
        }
        log.info("[GlobalBroadcaster] broadcastToAll '{}' → {} clients", response.getType(), count);
    }

    /**
     * Gửi tín hiệu chỉ đến ADMIN / MANAGER.
     * Dùng cho: RELOAD_PENDING_ITEMS, RELOAD_ALL_ITEMS.
     */
    public void broadcastToAdmins(Response<?> response) {
        int count = 0;
        for (ClientContext ctx : adminClients) {
            try {
                ctx.sendResponse(response);
                count++;
            } catch (Exception e) {
                log.warn("[GlobalBroadcaster] Không thể gửi đến admin {}: {}", ctx.getClientId(), e.getMessage());
            }
        }
        log.info("[GlobalBroadcaster] broadcastToAdmins '{}' → {} admins", response.getType(), count);
    }

    /**
     * Gửi tín hiệu đến một client cụ thể theo accountName.
     * Dùng cho: notify seller khi sản phẩm được duyệt/từ chối.
     */
    public void broadcastToClient(String accountName, Response<?> response) {
        for (ClientContext ctx : allClients) {
            if (accountName.equals(ctx.getClientId())) {
                try {
                    ctx.sendResponse(response);
                    log.info("[GlobalBroadcaster] Sent '{}' to client: {}", response.getType(), accountName);
                } catch (Exception e) {
                    log.warn("[GlobalBroadcaster] Không thể gửi đến {}: {}", accountName, e.getMessage());
                }
                return;
            }
        }
        log.debug("[GlobalBroadcaster] Client {} không online, bỏ qua broadcast.", accountName);
    }
}