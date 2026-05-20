package com.tboat.socket.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.ParticipationDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.auction.AuctionSession;
import com.tboat.models.auction.Participation;
import com.tboat.models.network.Response;
import com.tboat.models.auction.StatusOfAuction;
import com.tboat.service.AuctionManager;
import com.tboat.service.AuctionRoom;
import com.tboat.service.NotificationService;
import com.tboat.service.SellerService;
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
        context.sendResponse(new Response<>("LIST_AVAILABLE", "SUCCESS",
                "Danh sách phiên đấu giá", auctionDAO.getAvailableAuctions()));
    }

    public void join(String raw) {
        try {
            int sessionId = JsonParser.parseString(raw)
                    .getAsJsonObject().get("payload").getAsInt();

            AuctionSession session = auctionDAO.getAuctionById(sessionId);
            if (session == null) {
                context.sendResponse(new Response<>("JOIN", "ERROR", "Phiên không tồn tại", null));
                return;
            }

            StatusOfAuction status = session.getStatusOfAuction();
            if (status == StatusOfAuction.ENDED || status == StatusOfAuction.CANCELED) {
                context.sendResponse(new Response<>("JOIN", "ERROR", "Phiên đã kết thúc hoặc bị hủy", null));
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
            joinData.addProperty("isSeller",       session.getSellerAccountName().equals(context.getClientId()));
            joinData.addProperty("sellerNickname", sellerNickname);
            if (leaderNickname != null)
                joinData.addProperty("leaderNickname", leaderNickname);

            context.sendResponse(new Response<>("JOIN", "JOIN_SUCCESS", "Vào phòng thành công", joinData));

        } catch (Exception e) {
            log.error("Lỗi JOIN [{}]: {}", context.getClientId(), e.getMessage(), e);
            context.sendResponse(new Response<>("JOIN", "ERROR", "Lỗi hệ thống khi vào phòng", null));
        }
    }

    public void bid(String raw) {
        AuctionRoom room = context.getCurrentRoom();
        if (room == null) {
            context.sendResponse(new Response<>("BID", "ERROR", "Bạn chưa vào phòng", null));
            return;
        }

        String clientId = context.getClientId();

        Participation role = participationDAO.getRoleType(clientId, room.getSessionId());
        if (role != null && "SELLER".equals(role.getRoleType())) {
            context.sendResponse(new Response<>("BID", "FAILED",
                    "Bạn không thể tự đặt giá cho sản phẩm của mình!", null));
            return;
        }

        double price = JsonParser.parseString(raw)
                .getAsJsonObject().get("payload").getAsDouble();

        // FIX: Lấy previousLeader từ room thay vì query DB lần đầu — tránh double query
        //      room.getLastBidder() đã được cập nhật sau mỗi lần bid thành công
        String previousLeader = room.getLastBidder();

        boolean accepted = room.placeBid(price, clientId);

        if (accepted) {
            if (role == null) {
                participationDAO.addParticipation(
                        new Participation(clientId, room.getSessionId(), "BIDDER"));
            }
            context.sendResponse(new Response<>("BID", "SUCCESS", "Bạn đang dẫn đầu", price));

            String leaderNickname = userDAO.getNickname(clientId);
            JsonObject bidData = new JsonObject();
            bidData.addProperty("newPrice",          price);
            bidData.addProperty("newLeader",         clientId);
            bidData.addProperty("newLeaderNickname", leaderNickname);
            room.broadcast("NEW_BID", leaderNickname + " vừa đặt giá mới", bidData);

            // Query DB một lần duy nhất sau bid — dùng cho cả notification và reload
            AuctionSession sessionAfterBid = auctionDAO.getAuctionById(room.getSessionId());
            if (sessionAfterBid != null) {
                NotificationService.getInstance().onNewBid(sessionAfterBid, clientId, price);

                if (previousLeader != null
                        && !previousLeader.isBlank()
                        && !previousLeader.equals(clientId)) {
                    NotificationService.getInstance().onOutbid(sessionAfterBid, previousLeader, price);
                }
            }

            // FIX: Broadcast cho admin và history biết có bid mới để reload dữ liệu
            GlobalBroadcaster.getInstance().broadcastToAdmins(
                    new Response<>("RELOAD_ALL_ITEMS", "NOTIFY",
                            "Có bid mới trong phiên " + room.getSessionId(), room.getSessionId()));

        } else {
            context.sendResponse(new Response<>("BID", "FAILED",
                    "Giá đặt phải cao hơn giá hiện tại", room.getCurrentPrice()));
        }
    }

    public void cancelAuction(String raw) {
        try {
            int sessionId = JsonParser.parseString(raw)
                    .getAsJsonObject().get("payload").getAsInt();

            boolean ok = sellerService.cancelAuction(context.getClientId(), sessionId);

            if (ok) {
                AuctionRoom room = AuctionManager.getInstance().getRoom(sessionId);
                if (room != null) {
                    room.broadcast("AUCTION_CANCELED",
                            "Phiên " + sessionId + " đã bị người bán hủy", sessionId);
                }

                AuctionManager.getInstance().removeRoom(sessionId);

                context.sendResponse(new Response<>("CANCEL_AUCTION", "SUCCESS",
                        "Đã hủy phiên đấu giá thành công", sessionId));

                if (context.getCurrentRoom() != null) {
                    context.getCurrentRoom().removeSubscriber(context);
                    context.setCurrentRoom(null);
                }

                // FIX: Broadcast reload trang chủ và admin sau khi hủy
                GlobalBroadcaster.getInstance().broadcastToAll(
                        new Response<>("RELOAD_AVAILABLE", "NOTIFY",
                                "Phiên " + sessionId + " đã bị hủy", sessionId));

                GlobalBroadcaster.getInstance().broadcastToAdmins(
                        new Response<>("RELOAD_ALL_ITEMS", "NOTIFY",
                                "Phiên " + sessionId + " đã bị người bán hủy", sessionId));

            } else {
                context.sendResponse(new Response<>("CANCEL_AUCTION", "ERROR",
                        "Không thể hủy (đã có người đặt giá)", null));
            }
        } catch (Exception e) {
            log.error("Lỗi CANCEL_AUCTION [{}]: {}", context.getClientId(), e.getMessage(), e);
            context.sendResponse(new Response<>("CANCEL_AUCTION", "ERROR",
                    "Dữ liệu yêu cầu hủy không hợp lệ", null));
        }
    }
}