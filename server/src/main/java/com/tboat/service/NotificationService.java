package com.tboat.service;

import com.tboat.dao.AuctionSessionDAO;
import com.tboat.models.auction.AuctionSession;
import com.tboat.models.network.Response;
import com.tboat.models.network.ServerEvent;
import com.tboat.socket.ClientContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NotificationService — broadcast thông báo đến đúng client qua socket.
 *
 * Pattern giống BiddingService / UserManager:
 *   - Singleton, không có state mutable ngoài registry
 *   - Không phụ thuộc JavaFX — server là pure Java
 *   - Client nhận JSON, tự gọi NotificationManager để hiển thị UI
 *
 * Registry: map username → ClientContext (đăng ký khi LOGIN, hủy khi LOGOUT/disconnect)
 */
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static final NotificationService INSTANCE = new NotificationService();
    private NotificationService() {}
    public static NotificationService getInstance() { return INSTANCE; }

    // ── Registry: username → context đang kết nối ────────────────────────────
    private final Map<String, ClientContext> onlineClients = new ConcurrentHashMap<>();

    private final AuctionSessionDAO sessionDAO = new AuctionSessionDAO();

    // ════════════════════════════════════════════════════════════════════════
    // REGISTRY — gọi trong AuthHandler
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Đăng ký client khi đăng nhập thành công.
     *
     * <pre>
     * // AuthHandler.login()
     * context.setClientId(username);
     * NotificationService.getInstance().register(username, context);
     * </pre>
     */
    public void register(String username, ClientContext context) {
        onlineClients.put(username, context);
        log.debug("[Notif] Đã đăng ký: {}", username);
    }

    /**
     * Hủy đăng ký khi logout hoặc mất kết nối.
     *
     * <pre>
     * // AuthHandler.logout() và ClientContext.cleanup()
     * NotificationService.getInstance().unregister(username);
     * </pre>
     */
    public void unregister(String username) {
        onlineClients.remove(username);
        log.debug("[Notif] Đã hủy đăng ký: {}", username);
    }

    // ════════════════════════════════════════════════════════════════════════
    // PUBLIC API — gọi trong Handler/Service sau khi xử lý business logic
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Gọi trong AuctionHandler.bid() — sau khi room.placeBid() thành công.
     * Gửi đến người bán của phiên: có bid mới.
     *
     * <pre>
     * // AuctionHandler.bid()
     * if (accepted) {
     *     AuctionSession session = auctionDAO.getAuctionById(room.getSessionId());
     *     NotificationService.getInstance()
     *         .onNewBid(session, clientId, price);
     * }
     * </pre>
     */
    public void onNewBid(AuctionSession session, String bidderName, double price) {
        String seller = session.getSellerAccountName();
        push(seller, "NEW_BID",
                bidderName + " vừa đặt giá " + formatMoney(price),
                session.getName(),
                getInitials(bidderName), "#DCE8FD");
    }

    /**
     * Gọi trong AuctionHandler.bid() — thông báo cho người vừa bị vượt giá.
     *
     * <pre>
     * // AuctionHandler.bid()
     * String previousLeader = room.getCurrentLeader();
     * if (accepted && previousLeader != null && !previousLeader.equals(clientId)) {
     *     NotificationService.getInstance()
     *         .onOutbid(session, previousLeader, price);
     * }
     * </pre>
     */
    public void onOutbid(AuctionSession session, String outbidUser, double newPrice) {
        push(outbidUser, "OUTBID",
                "Bạn vừa bị vượt giá!",
                session.getName() + " — giá mới: " + formatMoney(newPrice),
                "⚡", "#FFF3E0");
    }

    /**
     * Gọi trong AuctionTimerService khi đếm ngược đến mốc cảnh báo.
     * Gửi đến người bán + tất cả bidder đang online trong phiên.
     *
     * <pre>
     * // AuctionTimerService
     * if (minutesLeft == 60 || minutesLeft == 30) {
     *     AuctionSession session = sessionDAO.getAuctionById(sessionId);
     *     List<String> participants = participationDAO.getParticipants(sessionId);
     *     NotificationService.getInstance()
     *         .onAuctionEnding(session, participants, minutesLeft);
     * }
     * </pre>
     */
    public void onAuctionEnding(AuctionSession session,
                                java.util.List<String> participants,
                                int minutesLeft) {
        String timeText = minutesLeft >= 60
                ? (minutesLeft / 60) + " giờ"
                : minutesLeft + " phút";
        String subtitle = session.getName() + " — còn " + timeText + " nữa";

        // Thông báo tất cả người liên quan
        for (String username : participants) {
            push(username, "AUCTION_ENDING",
                    "Phiên đấu giá sắp kết thúc!", subtitle, "⏰", "#FFF3E0");
        }
    }

    /**
     * Gọi trong AuctionRoom.closeSession() — thông báo người thắng.
     *
     * <pre>
     * // AuctionRoom.closeSession()
     * if (winner != null) {
     *     NotificationService.getInstance()
     *         .onAuctionWon(session, winner, finalPrice);
     * }
     * </pre>
     */
    public void onAuctionWon(AuctionSession session, String winner, double finalPrice) {
        push(winner, "AUCTION_WON",
                "🏆 Bạn đã thắng phiên đấu giá!",
                session.getName() + " — " + formatMoney(finalPrice),
                "🏆", "#E6F4EA");
    }

    /**
     * Gọi trong AuctionRoom.closeSession() — thông báo người bán đã bán được.
     *
     * <pre>
     * // AuctionRoom.closeSession()
     * NotificationService.getInstance()
     *     .onAuctionSold(session, winner, finalPrice);
     * </pre>
     */
    public void onAuctionSold(AuctionSession session, String winner, double finalPrice) {
        String seller = session.getSellerAccountName();
        push(seller, "AUCTION_SOLD",
                "Sản phẩm đã được bán thành công!",
                session.getName() + " — " + winner + " mua với giá " + formatMoney(finalPrice),
                "💰", "#E6F4EA");
    }

    /**
     * Gọi trong AuctionHandler.cancelAuction() — thông báo các bidder khi bị hủy.
     *
     * <pre>
     * // AuctionHandler.cancelAuction()
     * if (ok) {
     *     List<String> bidders = participationDAO.getBidders(sessionId);
     *     NotificationService.getInstance()
     *         .onAuctionCanceled(session, bidders);
     * }
     * </pre>
     */
    public void onAuctionCanceled(AuctionSession session,
                                  java.util.List<String> bidders) {
        for (String bidder : bidders) {
            push(bidder, "AUCTION_CANCELED",
                    "Phiên đấu giá đã bị hủy",
                    session.getName(),
                    "❌", "#FDECEA");
        }
    }

    /**
     * Gọi trong ItemHandler.approveItem() — thông báo người bán được duyệt.
     *
     * <pre>
     * // ItemHandler.approveItem()
     * if (approved) {
     *     NotificationService.getInstance()
     *         .onItemApproved(sellerUsername, item.getName());
     * }
     * </pre>
     */
    public void onItemApproved(String sellerUsername, String itemName) {
        push(sellerUsername, "ITEM_APPROVED",
                "Sản phẩm được duyệt thành công!",
                itemName + " — đã sẵn sàng đấu giá",
                "✅", "#E6F4EA");
    }

    /**
     * Gọi trong ItemHandler.rejectItem() — thông báo người bán bị từ chối.
     *
     * <pre>
     * // ItemHandler.rejectItem()
     * NotificationService.getInstance()
     *     .onItemRejected(sellerUsername, item.getName(), reason);
     * </pre>
     */
    public void onItemRejected(String sellerUsername, String itemName, String reason) {
        push(sellerUsername, "ITEM_REJECTED",
                "Sản phẩm bị từ chối duyệt",
                itemName + " — " + reason,
                "⚠️", "#FFF3E0");
    }


    /**
     * Gọi trong UserHandler.transaction() khi amount > 0 — nạp tiền thành công.
     *
     * <pre>
     * // UserHandler.java
     * if (amount > 0) {
     *     NotificationService.getInstance()
     *         .onDeposit(context.getClientId(), amount, newBalance);
     * }
     * </pre>
     */
    public void onDeposit(String username, double amount, double newBalance) {
        push(username, "DEPOSIT",
                "Nạp tiền thành công!",
                "+" + formatMoney(amount) + " — Số dư: " + formatMoney(newBalance),
                "💳", "#E6F4EA");
    }

    /**
     * Gọi trong UserHandler.transaction() khi amount < 0 — rút tiền thành công.
     *
     * <pre>
     * // UserHandler.java
     * } else {
     *     NotificationService.getInstance()
     *         .onWithdraw(context.getClientId(), Math.abs(amount), newBalance);
     * }
     * </pre>
     */
    public void onWithdraw(String username, double amount, double newBalance) {
        push(username, "WITHDRAW",
                "Rút tiền thành công!",
                "-" + formatMoney(amount) + " — Số dư: " + formatMoney(newBalance),
                "🏧", "#FFF3E0");
    }
    // ════════════════════════════════════════════════════════════════════════
    // INTERNAL HELPERS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Gửi thông báo đến một user — nếu offline thì bỏ qua (không queue).
     * Payload theo cấu trúc mà NotificationManager trên client hiểu được.
     */
    private void push(String username, String action,
                      String title, String subtitle,
                      String avatarText, String avatarColor) {
        ClientContext ctx = onlineClients.get(username);
        if (ctx == null) {
            log.debug("[Notif] {} offline — bỏ qua thông báo '{}'", username, title);
            return;
        }

        // Payload khớp với NotificationItem constructor bên client
        com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
        payload.addProperty("notifType",    action);
        payload.addProperty("title",       title);
        payload.addProperty("subtitle",    subtitle);
        payload.addProperty("avatarText",  avatarText);
        payload.addProperty("avatarColor", avatarColor);

        ctx.sendResponse(new Response<>(ServerEvent.NOTIFICATION.name(), ServerEvent.SYSTEM.name(), title, payload));
        log.debug("[Notif] Đã gửi '{}' → {}", action, username);
    }

    /** Lấy 2 chữ cái đầu của tên — dùng làm avatar. */
    private String getInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1)
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    /** Format tiền VND — giống cách hiển thị trong BiddingService. */
    private String formatMoney(double amount) {
        String raw = String.valueOf((long) amount);
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = raw.length() - 1; i >= 0; i--) {
            if (count > 0 && count % 3 == 0) sb.insert(0, '.');
            sb.insert(0, raw.charAt(i));
            count++;
        }
        return sb + " ₫";
    }
}
