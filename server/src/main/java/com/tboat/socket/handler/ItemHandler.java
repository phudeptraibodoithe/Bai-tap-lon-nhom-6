package com.tboat.socket.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.ItemDAO;
import com.tboat.dao.ParticipationDAO;
import com.tboat.models.auction.AuctionSession;
import com.tboat.models.auction.Participation;
import com.tboat.models.auction.StatusOfAuction;
import com.tboat.models.item.Item;
import com.tboat.models.item.factory.ItemFactory;
import com.tboat.models.item.factory.ItemFactoryProducer;
import com.tboat.models.network.Response;
import com.tboat.service.AuctionManager;
import com.tboat.service.AuctionRoom;
import com.tboat.service.AuctionTimerService;
import com.tboat.service.NotificationService;
import com.tboat.service.SellerService;
import com.tboat.socket.ClientContext;
import com.tboat.socket.GlobalBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class ItemHandler {

    private static final Logger log = LoggerFactory.getLogger(ItemHandler.class);

    private final ClientContext     context;
    private final AuctionSessionDAO auctionDAO       = new AuctionSessionDAO();
    private final ItemDAO           itemDAO          = new ItemDAO();
    private final ParticipationDAO  participationDAO = new ParticipationDAO();
    private final SellerService     sellerService    = new SellerService();

    public ItemHandler(ClientContext context) { this.context = context; }

    public void getAllItems() {
        try {
            var list = auctionDAO.getAllAuctions();
            context.sendResponse(new Response<>("GET_ALL_ITEMS", "SUCCESS",
                    "Danh sách tất cả phiên", list != null ? list : new ArrayList<>()));
        } catch (Exception e) {
            log.error("Lỗi GET_ALL_ITEMS: {}", e.getMessage(), e);
            context.sendResponse(new Response<>("GET_ALL_ITEMS", "ERROR",
                    "Lỗi lấy danh sách: " + e.getMessage(), null));
        }
    }

    public void postItem(String raw) {
        try {
            JsonObject payload = JsonParser.parseString(raw)
                    .getAsJsonObject().getAsJsonObject("payload");

            Item item = buildItemFromPayload(payload, context.getClientId());

            int itemId = itemDAO.addItem(item);
            if (itemId == -1) {
                context.sendResponse(new Response<>("POST_ITEM", "ERROR",
                        "Lỗi lưu item vào Database", null));
                return;
            }

            AuctionSession session = buildSessionFromPayload(payload, item);
            session.setItemId(itemId);
            session.setStatusOfAuction(StatusOfAuction.PENDING);

            int sessionId = auctionDAO.addAuctionSession(session);
            if (sessionId > 0) {
                participationDAO.addParticipation(
                        new Participation(context.getClientId(), sessionId, "SELLER"));

                context.sendResponse(new Response<>("POST_ITEM", "SUCCESS",
                        "Đăng sản phẩm thành công, đang chờ duyệt", sessionId));

                // FIX: Broadcast cho admin và manager biết có sản phẩm mới cần duyệt
                GlobalBroadcaster.getInstance().broadcastToAdmins(
                        new Response<>("RELOAD_PENDING_ITEMS", "NOTIFY",
                                "Có sản phẩm mới chờ duyệt từ " + context.getClientId(), sessionId));

            } else {
                context.sendResponse(new Response<>("POST_ITEM", "ERROR",
                        "Lỗi lưu phiên đấu giá vào Database", null));
            }
        } catch (Exception e) {
            log.error("Lỗi POST_ITEM [{}]: {}", context.getClientId(), e.getMessage(), e);
            context.sendResponse(new Response<>("POST_ITEM", "ERROR",
                    "Dữ liệu gửi lên không hợp lệ!", null));
        }
    }

    public void editItem(String raw) {
        try {
            JsonObject payload = JsonParser.parseString(raw)
                    .getAsJsonObject().getAsJsonObject("payload");

            int sessionId = payload.get("id").getAsInt();

            AuctionSession existing = auctionDAO.getAuctionById(sessionId);
            if (existing == null) {
                context.sendResponse(new Response<>("EDIT_ITEM", "ERROR",
                        "Không tìm thấy phiên đấu giá", null));
                return;
            }

            Item updatedItem = buildItemFromPayload(payload, context.getClientId());
            boolean itemOk = itemDAO.updateItem(updatedItem, existing.getItemId());
            if (!itemOk) {
                context.sendResponse(new Response<>("EDIT_ITEM", "ERROR",
                        "Lỗi cập nhật item", null));
                return;
            }

            AuctionSession updated = buildSessionFromPayload(payload, updatedItem);
            updated.setId(sessionId);
            updated.setItemId(existing.getItemId());

            boolean ok = sellerService.editAuction(context.getClientId(), updated);
            if (ok) {
                context.sendResponse(new Response<>("EDIT_ITEM", "SUCCESS",
                        "Cập nhật thông tin sản phẩm thành công!", sessionId));

                // FIX: Load lại session mới từ DB sau khi edit để có startTime/endTime chính xác
                AuctionSession refreshed = auctionDAO.getAuctionById(sessionId);
                if (refreshed != null) {

                    // FIX: Reschedule timer theo thời gian mới (tránh timer cũ chạy sai)
                    AuctionTimerService.getInstance().scheduleAuction(refreshed);
                    log.info("[ItemHandler] Đã reschedule timer cho phiên {} sau khi edit.", sessionId);

                    // FIX: Nếu phiên đang ONGOING, broadcast TIME_UPDATED về room để client reset countdown
                    if (refreshed.getStatusOfAuction() == StatusOfAuction.ONGOING) {
                        AuctionRoom room = AuctionManager.getInstance().getRoom(sessionId);
                        if (room != null) {
                            JsonObject timeData = new JsonObject();
                            timeData.addProperty("newStartTime", refreshed.getStartTime().toString());
                            timeData.addProperty("newEndTime",   refreshed.getEndTime().toString());
                            room.broadcast("TIME_UPDATED",
                                    "Thời gian phiên đấu giá đã được cập nhật", timeData);
                        }
                    }

                    // FIX: Broadcast reload cho admin/manager để cập nhật danh sách
                    GlobalBroadcaster.getInstance().broadcastToAdmins(
                            new Response<>("RELOAD_ALL_ITEMS", "NOTIFY",
                                    "Phiên " + sessionId + " vừa được cập nhật", sessionId));
                }

            } else {
                context.sendResponse(new Response<>("EDIT_ITEM", "ERROR",
                        "Không thể cập nhật: Phiên đã bắt đầu, đã có người bid hoặc lỗi quyền sở hữu.", null));
            }
        } catch (Exception e) {
            log.error("Lỗi EDIT_ITEM [{}]: {}", context.getClientId(), e.getMessage(), e);
            context.sendResponse(new Response<>("EDIT_ITEM", "ERROR",
                    "Dữ liệu gửi lên không hợp lệ!", null));
        }
    }

    public void approveItem(String raw) {
        try {
            int sessionId = JsonParser.parseString(raw)
                    .getAsJsonObject().get("payload").getAsInt();

            auctionDAO.updateSessionStatus(sessionId, StatusOfAuction.NOT_STARTED);

            AuctionSession session = auctionDAO.getAuctionById(sessionId);

            if (session != null) {
                AuctionTimerService.getInstance().scheduleAuction(session);

                context.sendResponse(new Response<>("APPROVE_ITEM", "SUCCESS",
                        "Đã duyệt và bắt đầu đấu giá", sessionId));

                NotificationService.getInstance().onItemApproved(
                        session.getSellerAccountName(), session.getName());

                // FIX: Broadcast reload trang chủ (LIST_AVAILABLE sẽ có phiên mới)
                //      và broadcast cho seller biết sản phẩm được duyệt
                GlobalBroadcaster.getInstance().broadcastToAll(
                        new Response<>("RELOAD_AVAILABLE", "NOTIFY",
                                "Có phiên đấu giá mới vừa được duyệt", sessionId));

                log.info("[Server] Admin duyệt phiên ID: {}", sessionId);
            } else {
                context.sendResponse(new Response<>("APPROVE_ITEM", "ERROR",
                        "Không tìm thấy sản phẩm cần duyệt", null));
            }
        } catch (Exception e) {
            log.error("Lỗi APPROVE_ITEM: {}", e.getMessage(), e);
            context.sendResponse(new Response<>("APPROVE_ITEM", "ERROR",
                    "Lỗi xử lý duyệt: " + e.getMessage(), null));
        }
    }

    public void rejectItem(String raw) {
        try {
            int sessionId = JsonParser.parseString(raw)
                    .getAsJsonObject().get("payload").getAsInt();

            boolean ok = auctionDAO.updateSessionStatus(sessionId, StatusOfAuction.CANCELED);

            if (ok) {
                AuctionSession session = auctionDAO.getAuctionById(sessionId);

                context.sendResponse(new Response<>("REJECT_ITEM", "SUCCESS",
                        "Đã từ chối sản phẩm", sessionId));

                if (session != null) {
                    NotificationService.getInstance().onItemRejected(
                            session.getSellerAccountName(), session.getName(), "Không đạt yêu cầu duyệt");
                }

                // FIX: Broadcast reload cho admin để cập nhật danh sách pending
                GlobalBroadcaster.getInstance().broadcastToAdmins(
                        new Response<>("RELOAD_PENDING_ITEMS", "NOTIFY",
                                "Phiên " + sessionId + " vừa bị từ chối", sessionId));

                log.info("[Server] Admin từ chối phiên ID: {}", sessionId);
            } else {
                context.sendResponse(new Response<>("REJECT_ITEM", "ERROR",
                        "Không thể thực hiện từ chối (Lỗi Database)", null));
            }
        } catch (Exception e) {
            log.error("Lỗi REJECT_ITEM: {}", e.getMessage(), e);
            context.sendResponse(new Response<>("REJECT_ITEM", "ERROR",
                    "Lỗi xử lý từ chối: " + e.getMessage(), null));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Item buildItemFromPayload(JsonObject p, String seller) {
        ItemFactory factory = ItemFactoryProducer.getFactory(p.get("type").getAsString());
        return factory.createItem(
                seller,
                p.get("name").getAsString(),
                p.get("description").getAsString(),
                p.get("imageURL").getAsString()
        );
    }

    private AuctionSession buildSessionFromPayload(JsonObject p, Item item) {
        AuctionSession session = new AuctionSession(
                LocalDateTime.parse(p.get("startTime").getAsString()),
                LocalDateTime.parse(p.get("endTime").getAsString()),
                p.get("currentPrice").getAsDouble(),
                p.get("bidIncrease").getAsDouble(),
                item
        );
        session.setBuyNowPrice(p.has("buyNowPrice") && !p.get("buyNowPrice").isJsonNull()
                ? p.get("buyNowPrice").getAsDouble() : 0.0);
        return session;
    }
}