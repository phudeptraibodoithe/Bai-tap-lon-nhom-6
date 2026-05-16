package com.tboat.socket.handler;

import com.google.gson.*;
import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.ParticipationDAO;
import com.tboat.models.*;
import com.tboat.service.*;
import com.tboat.socket.ClientContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class ItemHandler {

    private static final Logger log = LoggerFactory.getLogger(ItemHandler.class);

    private final ClientContext    context;
    private final AuctionSessionDAO auctionDAO       = new AuctionSessionDAO();
    private final ParticipationDAO  participationDAO = new ParticipationDAO();
    private final SellerService     sellerService    = new SellerService();

    public ItemHandler(ClientContext context) { this.context = context; }

    public void getPendingItems() {
        try {
            var list = auctionDAO.getPendingAuctions();
            context.sendResponse(new Response<>("SUCCESS", "Danh sách chờ duyệt",
                    list != null ? list : new ArrayList<>()));
        } catch (Exception e) {
            log.error("Lỗi GET_PENDING_ITEMS: {}", e.getMessage(), e);
            context.sendResponse(new Response<>("ERROR", "Lỗi lấy danh sách: " + e.getMessage(), null));
        }
    }

    public void postItem(String raw) {
        try {
            JsonObject payload = JsonParser.parseString(raw)
                    .getAsJsonObject().getAsJsonObject("payload");

            AuctionSession session = buildSessionFromPayload(payload, context.getClientId());
            session.setStatusOfAuction(StatusOfAuction.PENDING);

            int id = auctionDAO.addAuctionSession(session);
            if (id > 0) {
                participationDAO.addParticipation(
                        new Participation(context.getClientId(), id, "SELLER"));
                context.sendResponse(new Response<>("SUCCESS",
                        "Đăng sản phẩm thành công, đang chờ duyệt", id));
            } else {
                context.sendResponse(new Response<>("ERROR", "Lỗi lưu dữ liệu vào Database", null));
            }
        } catch (Exception e) {
            log.error("Lỗi POST_ITEM [{}]: {}", context.getClientId(), e.getMessage(), e);
            context.sendResponse(new Response<>("ERROR", "Dữ liệu gửi lên không hợp lệ!", null));
        }
    }

    public void editItem(String raw) {
        try {
            JsonObject payload = JsonParser.parseString(raw)
                    .getAsJsonObject().getAsJsonObject("payload");

            AuctionSession updated = buildSessionFromPayload(payload, context.getClientId());
            updated.setId(payload.get("id").getAsInt());

            boolean ok = sellerService.editAuction(context.getClientId(), updated);
            if (ok) {
                context.sendResponse(new Response<>("SUCCESS",
                        "Cập nhật thông tin sản phẩm thành công!", updated.getId()));
            } else {
                context.sendResponse(new Response<>("ERROR",
                        "Không thể cập nhật: Phiên đã bắt đầu, đã có người bid hoặc lỗi quyền sở hữu.", null));
            }
        } catch (Exception e) {
            log.error("Lỗi EDIT_ITEM [{}]: {}", context.getClientId(), e.getMessage(), e);
            context.sendResponse(new Response<>("ERROR", "Dữ liệu gửi lên không hợp lệ!", null));
        }
    }

    public void approveItem(String raw) {
        try {
            int sessionId = JsonParser.parseString(raw)
                    .getAsJsonObject().get("payload").getAsInt();
            AuctionSession session = auctionDAO.getAuctionById(sessionId);

            if (session != null) {
                AuctionTimerService.getInstance().scheduleAuction(session);
                context.sendResponse(new Response<>("SUCCESS",
                        "Đã duyệt! Hệ thống sẽ tự động canh giờ.", sessionId));
                log.info("[Server] Admin duyệt phiên ID: {}", sessionId);
            } else {
                context.sendResponse(new Response<>("ERROR",
                        "Không tìm thấy sản phẩm cần duyệt", null));
            }
        } catch (Exception e) {
            log.error("Lỗi APPROVE_ITEM: {}", e.getMessage(), e);
            context.sendResponse(new Response<>("ERROR", "Lỗi xử lý duyệt: " + e.getMessage(), null));
        }
    }

    public void rejectItem(String raw) {
        try {
            int sessionId = JsonParser.parseString(raw)
                    .getAsJsonObject().get("payload").getAsInt();
            boolean ok = auctionDAO.updateSessionStatus(sessionId, StatusOfAuction.CANCELED);

            if (ok) {
                context.sendResponse(new Response<>("SUCCESS", "Đã từ chối sản phẩm", sessionId));
                log.info("[Server] Admin từ chối phiên ID: {}", sessionId);
            } else {
                context.sendResponse(new Response<>("ERROR",
                        "Không thể thực hiện từ chối (Lỗi Database)", null));
            }
        } catch (Exception e) {
            log.error("Lỗi REJECT_ITEM: {}", e.getMessage(), e);
            context.sendResponse(new Response<>("ERROR", "Lỗi xử lý từ chối: " + e.getMessage(), null));
        }
    }

    // ── Helper dùng chung cho postItem và editItem ───────────────────────

    private AuctionSession buildSessionFromPayload(JsonObject p, String seller) {
        AuctionFactory factory = AuctionFactoryProducer.getFactory(p.get("type").getAsString());
        return factory.createAuctionSession(
                LocalDateTime.parse(p.get("startTime").getAsString()),
                LocalDateTime.parse(p.get("endTime").getAsString()),
                p.get("currentPrice").getAsDouble(),
                p.get("bidIncrease").getAsDouble(),
                seller,
                p.get("name").getAsString(),
                p.get("description").getAsString(),
                p.get("imageURL").getAsString()
        );
    }
}