package com.tboat.socket.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.ParticipationDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.AuctionSession;
import com.tboat.models.Participation;
import com.tboat.models.Response;
import com.tboat.models.StatusOfAuction;
import com.tboat.service.AuctionManager;
import com.tboat.service.AuctionRoom;
import com.tboat.service.SellerService;
import com.tboat.socket.ClientContext;
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

            JsonObject joinData = new JsonObject();
            joinData.addProperty("currentPrice", room.getCurrentPrice());
            joinData.addProperty("isSeller",
                    session.getSellerAccountName().equals(context.getClientId()));

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

//Không cần pre-check — BidderRole xử lý trong transaction nguyên tử
        boolean accepted = room.placeBid(price, clientId);

        if (accepted) {
            if (role == null) {
                participationDAO.addParticipation(
                        new Participation(clientId, room.getSessionId(), "BIDDER"));
            }
            context.sendResponse(new Response<>("BID", "SUCCESS", "Bạn đang dẫn đầu", price));

            JsonObject bidData = new JsonObject();
            bidData.addProperty("newPrice", price);
            bidData.addProperty("newLeader", clientId);
            room.broadcast("NEW_BID", clientId + " vừa đặt giá mới", bidData);
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
                //Lấy phòng TRƯỚC KHI xóa để còn broadcast được
                AuctionRoom room = AuctionManager.getInstance().getRoom(sessionId);
                if (room != null) {
                    room.broadcast("AUCTION_CANCELED",
                            "Phiên " + sessionId + " đã bị người bán hủy", sessionId);
                }

                //Xóa phòng SAU KHI đã broadcast
                AuctionManager.getInstance().removeRoom(sessionId);

                //Gửi phản hồi cho chính người gọi lệnh
                context.sendResponse(new Response<>("CANCEL_AUCTION", "SUCCESS",
                        "Đã hủy phiên đấu giá thành công", sessionId));

                //Xóa client này ra khỏi phòng
                if (context.getCurrentRoom() != null) {
                    context.getCurrentRoom().removeSubscriber(context);
                    context.setCurrentRoom(null);
                }

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