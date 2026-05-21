package com.tboat.socket.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.ParticipationDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.auction.AuctionSession;
import com.tboat.models.auction.BidResult;
import com.tboat.models.auction.Participation;
import com.tboat.models.network.Response;
import com.tboat.models.network.ServerEvent;
import com.tboat.models.auction.StatusOfAuction;
import com.tboat.service.*;
import com.tboat.socket.ClientContext;
import com.tboat.socket.GlobalBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuctionHandler {

    private static final Logger log = LoggerFactory.getLogger(AuctionHandler.class);

    private final ClientContext      context;
    private final AuctionSessionDAO  auctionDAO       = new AuctionSessionDAO();
    private final ParticipationDAO   participationDAO = new ParticipationDAO();
    private final UserDAO            userDAO          = new UserDAO();
    private final SellerService      sellerService    = new SellerService();

    public AuctionHandler(ClientContext context) { this.context = context; }

    public void listAvailable() {
        context.sendResponse(new Response<>(ServerEvent.LIST_AVAILABLE.name(), ServerEvent.SUCCESS.name(),
                "Danh sách phiên đấu giá", auctionDAO.getAvailableAuctions()));
    }

    public void join(String raw) {
        try {
            int sessionId = JsonParser.parseString(raw)
                    .getAsJsonObject().get("payload").getAsInt();

            AuctionSession session = auctionDAO.getAuctionById(sessionId);
            if (session == null) {
                context.sendResponse(new Response<>(ServerEvent.JOIN.name(), ServerEvent.ERROR.name(),
                        "Phiên không tồn tại", null));
                return;
            }

            StatusOfAuction status = session.getStatusOfAuction();
            if (status == StatusOfAuction.ENDED || status == StatusOfAuction.CANCELED) {
                context.sendResponse(new Response<>(ServerEvent.JOIN.name(), ServerEvent.ERROR.name(),
                        "Phiên đã kết thúc hoặc bị hủy", null));
                return;
            }

            AuctionManager mgr = AuctionManager.getInstance();
            AuctionRoom room = mgr.getRoom(sessionId);
            if (room == null) {
                mgr.createRoom(sessionId, session.getCurrentPrice());
                room = mgr.getRoom(sessionId);
            }

            AuctionRoom old = context.getCurrentRoom();
            if (old != null) old.removeSubscriber(context);

            context.setCurrentRoom(room);
            room.addSubscriber(context);

            String sellerNickname = userDAO.getNickname(session.getSellerAccountName());
            String leaderNickname = null;
            String highestBidder  = session.getHighestBidderAccount();
            if (highestBidder != null && !highestBidder.isBlank())
                leaderNickname = userDAO.getNickname(highestBidder);

            JsonObject joinData = new JsonObject();
            joinData.addProperty("currentPrice",   room.getCurrentPrice());
            joinData.addProperty("buyNowPrice",    session.getBuyNowPrice());
            joinData.addProperty("isSeller",       session.getSellerAccountName().equals(context.getClientId()));
            joinData.addProperty("sellerNickname", sellerNickname);
            if (leaderNickname != null)
                joinData.addProperty("leaderNickname", leaderNickname);

            context.sendResponse(new Response<>(ServerEvent.JOIN.name(), ServerEvent.JOIN_SUCCESS.name(),
                    "Vào phòng thành công", joinData));

        } catch (Exception e) {
            log.error("Lỗi JOIN [{}]: {}", context.getClientId(), e.getMessage(), e);
            context.sendResponse(new Response<>(ServerEvent.JOIN.name(), ServerEvent.ERROR.name(),
                    "Lỗi hệ thống khi vào phòng", null));
        }
    }

    public void bid(String raw) {
        AuctionRoom room = context.getCurrentRoom();
        if (room == null) {
            context.sendResponse(new Response<>(ServerEvent.BID.name(), ServerEvent.ERROR.name(),
                    "Bạn chưa vào phòng", null));
            return;
        }

        String clientId = context.getClientId();

        Participation role = participationDAO.getRoleType(clientId, room.getSessionId());
        if (role != null && "SELLER".equals(role.getRoleType())) {
            context.sendResponse(new Response<>(ServerEvent.BID.name(), ServerEvent.FAILED.name(),
                    "Bạn không thể tự đặt giá cho sản phẩm của mình!", null));
            return;
        }

        double price;
        try {
            price = JsonParser.parseString(raw).getAsJsonObject().get("payload").getAsDouble();
        } catch (Exception e) {
            context.sendResponse(new Response<>(ServerEvent.BID.name(), ServerEvent.ERROR.name(),
                    "Dữ liệu bid không hợp lệ", null));
            return;
        }

        String previousLeader = room.getLastBidder();
        BidResult result = room.placeBid(price, clientId);

        switch (result) {
            case OK -> {
                if (role == null)
                    participationDAO.addParticipation(
                            new Participation(clientId, room.getSessionId(), "BIDDER"));

                context.sendResponse(new Response<>(ServerEvent.BID.name(), ServerEvent.SUCCESS.name(),
                        "Bạn đang dẫn đầu", price));

                String nick = userDAO.getNickname(clientId);
                JsonObject bidData = new JsonObject();
                bidData.addProperty("newPrice",          price);
                bidData.addProperty("newLeader",         clientId);
                bidData.addProperty("newLeaderNickname", nick);
                bidData.addProperty("bidTime",           java.time.LocalDateTime.now().toString());
                room.broadcast(ServerEvent.NEW_BID.name(), nick + " vừa đặt giá mới", bidData);

                AuctionSession session = auctionDAO.getAuctionById(room.getSessionId());
                if (session != null) {
                    NotificationService.getInstance().onNewBid(session, clientId, price);
                    if (previousLeader != null && !previousLeader.isBlank()
                            && !previousLeader.equals(clientId))
                        NotificationService.getInstance().onOutbid(session, previousLeader, price);

                    if (session.getBuyNowPrice() > 0 && price >= session.getBuyNowPrice()) {
                        room.finishAuction();
                        return;
                    }
                }

                GlobalBroadcaster.getInstance().broadcastToAdmins(
                        new Response<>(ServerEvent.RELOAD_ALL_ITEMS.name(), ServerEvent.NOTIFY.name(),
                                "Có bid mới trong phiên " + room.getSessionId(), room.getSessionId()));
            }
            case INSUFFICIENT_BALANCE ->
                    context.sendResponse(new Response<>(ServerEvent.BID.name(), ServerEvent.FAILED.name(),
                            "Số dư không đủ để đặt giá này!", room.getCurrentPrice()));
            case PRICE_TOO_LOW -> {
                AuctionSession session = auctionDAO.getAuctionById(room.getSessionId());
                double minNext = (session != null)
                        ? room.getCurrentPrice() + session.getBidIncrease()
                        : room.getCurrentPrice();
                context.sendResponse(new Response<>(ServerEvent.BID.name(), ServerEvent.FAILED.name(),
                        "Giá tối thiểu là: " + minNext, room.getCurrentPrice()));
            }
            default ->
                    context.sendResponse(new Response<>(ServerEvent.BID.name(), ServerEvent.FAILED.name(),
                            "Đặt giá thất bại, vui lòng thử lại.", room.getCurrentPrice()));
        }
    }

    public void registerAutoBid(String raw) {
        AuctionRoom room = context.getCurrentRoom();
        if (room == null) {
            context.sendResponse(new Response<>(ServerEvent.REGISTER_AUTO_BID.name(), ServerEvent.ERROR.name(),
                    "Bạn chưa vào phòng", null));
            return;
        }

        String clientId = context.getClientId();

        Participation role = participationDAO.getRoleType(clientId, room.getSessionId());
        if (role != null && "SELLER".equals(role.getRoleType())) {
            context.sendResponse(new Response<>(ServerEvent.REGISTER_AUTO_BID.name(), ServerEvent.FAILED.name(),
                    "Người bán không thể đăng ký auto-bid!", null));
            return;
        }

        double maxBid;
        try {
            maxBid = JsonParser.parseString(raw)
                    .getAsJsonObject().get("payload")
                    .getAsJsonObject().get("maxBid").getAsDouble();
        } catch (Exception e) {
            context.sendResponse(new Response<>(ServerEvent.REGISTER_AUTO_BID.name(), ServerEvent.ERROR.name(),
                    "Dữ liệu không hợp lệ", null));
            return;
        }

        AuctionSession session = auctionDAO.getAuctionById(room.getSessionId());
        double minNext = (session != null)
                ? room.getCurrentPrice() + session.getBidIncrease()
                : room.getCurrentPrice();

        if (maxBid < minNext) {
            context.sendResponse(new Response<>(ServerEvent.REGISTER_AUTO_BID.name(), ServerEvent.FAILED.name(),
                    "maxBid phải cao hơn giá hiện tại tối thiểu: " + minNext, room.getCurrentPrice()));
            return;
        }

        if (role == null)
            participationDAO.addParticipation(
                    new Participation(clientId, room.getSessionId(), "BIDDER"));

        room.registerAutoBid(clientId, maxBid);

        context.sendResponse(new Response<>(ServerEvent.REGISTER_AUTO_BID.name(),
                ServerEvent.AUTO_BID_REGISTERED.name(),
                "Đã đăng ký auto-bid thành công với mức tối đa: " + maxBid, maxBid));
    }

    public void cancelAuction(String raw) {
        try {
            int sessionId = JsonParser.parseString(raw)
                    .getAsJsonObject().get("payload").getAsInt();

            boolean ok = sellerService.cancelAuction(context.getClientId(), sessionId);

            if (ok) {
                AuctionRoom room = AuctionManager.getInstance().getRoom(sessionId);
                if (room != null) {
                    room.broadcast(ServerEvent.AUCTION_CANCELED.name(),
                            "Phiên " + sessionId + " đã bị người bán hủy", sessionId);
                }

                AuctionManager.getInstance().removeRoom(sessionId);

                context.sendResponse(new Response<>(ServerEvent.CANCEL_AUCTION.name(), ServerEvent.SUCCESS.name(),
                        "Đã hủy phiên đấu giá thành công", sessionId));

                if (context.getCurrentRoom() != null) {
                    context.getCurrentRoom().removeSubscriber(context);
                    context.setCurrentRoom(null);
                }

                GlobalBroadcaster.getInstance().broadcastToAll(
                        new Response<>(ServerEvent.RELOAD_AVAILABLE.name(), ServerEvent.NOTIFY.name(),
                                "Phiên " + sessionId + " đã bị hủy", sessionId));
                GlobalBroadcaster.getInstance().broadcastToAdmins(
                        new Response<>(ServerEvent.RELOAD_ALL_ITEMS.name(), ServerEvent.NOTIFY.name(),
                                "Phiên " + sessionId + " đã bị người bán hủy", sessionId));

            } else {
                context.sendResponse(new Response<>(ServerEvent.CANCEL_AUCTION.name(), ServerEvent.ERROR.name(),
                        "Không thể hủy (đã có người đặt giá)", null));
            }
        } catch (Exception e) {
            log.error("Lỗi CANCEL_AUCTION [{}]: {}", context.getClientId(), e.getMessage(), e);
            context.sendResponse(new Response<>(ServerEvent.CANCEL_AUCTION.name(), ServerEvent.ERROR.name(),
                    "Dữ liệu yêu cầu hủy không hợp lệ", null));
        }
    }
}