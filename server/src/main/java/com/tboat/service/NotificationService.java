package com.tboat.service;

import com.tboat.models.auction.AuctionSession;
import com.tboat.models.network.Response;
import com.tboat.models.network.ServerEvent;
import com.tboat.socket.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final NotificationService INSTANCE = new NotificationService();
    private static final int MINUTES_PER_HOUR = 60;
    private static final int SINGLE_NAME_INITIAL_LIMIT = 2;
    private static final int MONEY_GROUP_SIZE = 3;
    private static final int FIRST_GROUP_SIZE = 0;
    private static final int LAST_INDEX_OFFSET = 1;

    private final Map<String, ClientSession> onlineClients = new ConcurrentHashMap<>();

    private NotificationService() {}

    public static NotificationService getInstance() {
        return INSTANCE;
    }

    public void register(String username, ClientSession context) {
        onlineClients.put(username, context);
        log.debug("[Notif] Đã đăng ký: {}", username);
    }

    public void unregister(String username) {
        onlineClients.remove(username);
        log.debug("[Notif] Đã hủy đăng ký: {}", username);
    }

    /*
     * Các phương thức thông báo chỉ dựng nội dung. Phương thức push bên dưới
     * chịu trách nhiệm gửi qua socket, nhờ vậy mọi loại thông báo có cùng dạng phản hồi.
     */
    public void onNewBid(AuctionSession session, String bidderName, double price) {
        push(session.getSellerAccountName(), new NotificationPayload(
                ServerEvent.NEW_BID,
                bidderName + " vừa đặt giá " + formatMoney(price),
                session.getName(),
                getInitials(bidderName),
                "#DCE8FD"
        ));
    }

    public void onOutbid(AuctionSession session, String outbidUser, double newPrice) {
        push(outbidUser, new NotificationPayload(
                ServerEvent.OUTBID,
                "Bạn vừa bị vượt giá!",
                session.getName() + " — giá mới: " + formatMoney(newPrice),
                "⚡",
                "#FFF3E0"
        ));
    }

    public void onAutoBidPlaced(AuctionSession session, String bidder, double price) {
        push(bidder, new NotificationPayload(
                ServerEvent.NEW_BID,
                "Auto-bid đã đặt giá mới",
                session.getName() + " — giá mới: " + formatMoney(price),
                "A",
                "#DCE8FD"
        ));
    }

    public void onAutoBidOut(AuctionSession session, String bidder, double currentPrice) {
        push(bidder, new NotificationPayload(
                ServerEvent.OUTBID,
                "Auto-bid đã dừng",
                session.getName() + " — giá hiện tại: " + formatMoney(currentPrice),
                "!",
                "#FFF3E0"
        ));
    }

    public void onAuctionEnding(AuctionSession session, List<String> participants, int minutesLeft) {
        String timeText = minutesLeft >= MINUTES_PER_HOUR
                ? (minutesLeft / MINUTES_PER_HOUR) + " giờ"
                : minutesLeft + " phút";
        String subtitle = session.getName() + " — còn " + timeText + " nữa";

        for (String username : participants) {
            push(username, new NotificationPayload(
                    ServerEvent.AUCTION_ENDING,
                    "Phiên đấu giá sắp kết thúc!",
                    subtitle,
                    "⏰",
                    "#FFF3E0"
            ));
        }
    }

    public void onAuctionWon(AuctionSession session, String winner, double finalPrice) {
        push(winner, new NotificationPayload(
                ServerEvent.AUCTION_WON,
                "🏆 Bạn đã thắng phiên đấu giá!",
                session.getName() + " — " + formatMoney(finalPrice),
                "🏆",
                "#E6F4EA"
        ));
    }

    public void onAuctionSold(AuctionSession session, String winner, double finalPrice) {
        push(session.getSellerAccountName(), new NotificationPayload(
                ServerEvent.AUCTION_SOLD,
                "Sản phẩm đã được bán thành công!",
                session.getName() + " — " + winner + " mua với giá " + formatMoney(finalPrice),
                "💰",
                "#E6F4EA"
        ));
    }

    public void onAuctionCanceled(AuctionSession session, List<String> bidders) {
        for (String bidder : bidders) {
            push(bidder, new NotificationPayload(
                    ServerEvent.AUCTION_CANCELED,
                    "Phiên đấu giá đã bị hủy",
                    session.getName(),
                    "❌",
                    "#FDECEA"
            ));
        }
    }

    public void onItemApproved(String sellerUsername, String itemName) {
        push(sellerUsername, new NotificationPayload(
                ServerEvent.ITEM_APPROVED,
                "Sản phẩm được duyệt thành công!",
                itemName + " — đã sẵn sàng đấu giá",
                "✅",
                "#E6F4EA"
        ));
    }

    public void onItemRejected(String sellerUsername, String itemName, String reason) {
        push(sellerUsername, new NotificationPayload(
                ServerEvent.ITEM_REJECTED,
                "Sản phẩm bị từ chối duyệt",
                itemName + " — " + reason,
                "!",
                "#FFF3E0"
        ));
    }

    public void onDeposit(String username, double amount, double newBalance) {
        push(username, new NotificationPayload(
                ServerEvent.DEPOSIT,
                "Nạp tiền thành công!",
                "+" + formatMoney(amount) + " — Số dư: " + formatMoney(newBalance),
                "💳",
                "#E6F4EA"
        ));
    }

    public void onWithdraw(String username, double amount, double newBalance) {
        push(username, new NotificationPayload(
                ServerEvent.WITHDRAW,
                "Rút tiền thành công!",
                "-" + formatMoney(amount) + " — Số dư: " + formatMoney(newBalance),
                "🏧",
                "#FFF3E0"
        ));
    }

    private void push(String username, NotificationPayload notification) {
        ClientSession client = onlineClients.get(username);
        if (client == null) {
            log.debug("[Notif] {} offline — bỏ qua thông báo '{}'", username, notification.title());
            return;
        }

        client.sendResponse(new Response<>(
                ServerEvent.NOTIFICATION,
                ServerEvent.SYSTEM,
                notification.title(),
                notification.toJson()
        ));
        log.debug("[Notif] Đã gửi '{}' → {}", notification.type().name(), username);
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] nameParts = fullName.trim().split("\\s+");
        if (nameParts.length == 1) {
            return nameParts[0].substring(0,
                    Math.min(SINGLE_NAME_INITIAL_LIMIT, nameParts[0].length())).toUpperCase();
        }
        return ("" + nameParts[0].charAt(0)
                + nameParts[nameParts.length - LAST_INDEX_OFFSET].charAt(0)).toUpperCase();
    }

    private String formatMoney(double amount) {
        String digits = String.valueOf((long) amount);
        StringBuilder text = new StringBuilder();
        int groupSize = FIRST_GROUP_SIZE;
        for (int i = digits.length() - LAST_INDEX_OFFSET; i >= 0; i--) {
            if (groupSize > FIRST_GROUP_SIZE && groupSize % MONEY_GROUP_SIZE == 0) text.insert(0, '.');
            text.insert(0, digits.charAt(i));
            groupSize++;
        }
        return text + " ₫";
    }
}
