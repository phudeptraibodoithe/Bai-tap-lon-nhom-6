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
import com.tboat.models.network.ServerEvent;
import com.tboat.service.AuctionManager;
import com.tboat.service.AuctionRoom;
import com.tboat.service.AuctionTimerService;
import com.tboat.service.NotificationService;
import com.tboat.service.SellerService;
import com.tboat.socket.ClientSession;
import com.tboat.socket.EventBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class ItemCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(ItemCommandHandler.class);

    private final ClientSession     context;
    private final AuctionSessionDAO auctionDAO       = new AuctionSessionDAO();
    private final ItemDAO           itemDAO          = new ItemDAO();
    private final ParticipationDAO  participationDAO = new ParticipationDAO();
    private final SellerService     sellerService    = new SellerService();

    public ItemCommandHandler(ClientSession context) { this.context = context; }

    public void getAllItems() {
        try {
            var auctions = auctionDAO.getAllAuctions();
            context.sendResponse(new Response<>(ServerEvent.GET_ALL_ITEMS.name(), ServerEvent.SUCCESS.name(),
                    "Danh sách tất cả phiên", auctions != null ? auctions : new ArrayList<>()));
        } catch (Exception e) {
            log.error("Lỗi GET_ALL_ITEMS: {}", e.getMessage(), e);
            context.sendResponse(new Response<>(ServerEvent.GET_ALL_ITEMS.name(), ServerEvent.ERROR.name(),
                    "Lỗi lấy danh sách: " + e.getMessage(), null));
        }
    }

    /*
     * Đăng hoặc sửa sản phẩm sẽ chạm hai bảng: item và auction.
     * JSON được parse tại đây, sau đó truyền object đầy đủ sang service/DAO.
     */
    public void postItem(String raw) {
        try {
            JsonObject payload = JsonParser.parseString(raw)
                    .getAsJsonObject().getAsJsonObject("payload");

            Item item = buildItemFromPayload(payload, context.getClientId());

            int itemId = itemDAO.addItem(item);
            if (itemId == -1) {
                context.sendResponse(new Response<>(ServerEvent.POST_ITEM.name(), ServerEvent.ERROR.name(),
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

                context.sendResponse(new Response<>(ServerEvent.POST_ITEM.name(), ServerEvent.SUCCESS.name(),
                        "Đăng sản phẩm thành công, đang chờ duyệt", sessionId));

                EventBroadcaster.getInstance().broadcastToAdmins(
                        new Response<>(ServerEvent.RELOAD_PENDING_ITEMS, ServerEvent.NOTIFY,
                                "Có sản phẩm mới chờ duyệt từ " + context.getClientId(), sessionId));
            } else {
                context.sendResponse(new Response<>(ServerEvent.POST_ITEM.name(), ServerEvent.ERROR.name(),
                        "Lỗi lưu phiên đấu giá vào Database", null));
            }
        } catch (Exception e) {
            log.error("Lỗi POST_ITEM [{}]: {}", context.getClientId(), e.getMessage(), e);
            context.sendResponse(new Response<>(ServerEvent.POST_ITEM.name(), ServerEvent.ERROR.name(),
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
                context.sendResponse(new Response<>(ServerEvent.EDIT_ITEM.name(), ServerEvent.ERROR.name(),
                        "Không tìm thấy phiên đấu giá", null));
                return;
            }

            Item updatedItem = buildItemFromPayload(payload, context.getClientId());
            boolean isItemSaved = itemDAO.updateItem(updatedItem, existing.getItemId());
            if (!isItemSaved) {
                context.sendResponse(new Response<>(ServerEvent.EDIT_ITEM.name(), ServerEvent.ERROR.name(),
                        "Lỗi cập nhật item", null));
                return;
            }

            AuctionSession updated = buildSessionFromPayload(payload, updatedItem);
            updated.setId(sessionId);
            updated.setItemId(existing.getItemId());

            boolean isAuctionSaved = sellerService.editAuction(context.getClientId(), updated);
            if (isAuctionSaved) {
                context.sendResponse(new Response<>(ServerEvent.EDIT_ITEM.name(), ServerEvent.SUCCESS.name(),
                        "Cập nhật thông tin sản phẩm thành công!", sessionId));

                // Tải lại session đã lưu để timer dùng startTime và endTime mới nhất.
                AuctionSession refreshed = auctionDAO.getAuctionById(sessionId);
                if (refreshed != null) {

                    AuctionTimerService.getInstance().scheduleAuction(refreshed);
                    log.info("[ItemCommandHandler] Đã reschedule timer cho phiên {} sau khi edit.", sessionId);

                    if (refreshed.getStatusOfAuction() == StatusOfAuction.ONGOING) {
                        AuctionRoom room = AuctionManager.getInstance().getRoom(sessionId);
                        if (room != null) {
                            JsonObject timePayload = new JsonObject();
                            timePayload.addProperty("newStartTime", refreshed.getStartTime().toString());
                            timePayload.addProperty("newEndTime",   refreshed.getEndTime().toString());
                            room.broadcast(ServerEvent.TIME_UPDATED,
                                    "Thời gian phiên đấu giá đã được cập nhật", timePayload);
                        }
                    }

                    EventBroadcaster.getInstance().broadcastToAdmins(
                            new Response<>(ServerEvent.RELOAD_ALL_ITEMS, ServerEvent.NOTIFY,
                                    "Phiên " + sessionId + " vừa được cập nhật", sessionId));
                }

            } else {
                context.sendResponse(new Response<>(ServerEvent.EDIT_ITEM.name(), ServerEvent.ERROR.name(),
                        "Không thể cập nhật: Phiên đã bắt đầu, đã có người bid hoặc lỗi quyền sở hữu.", null));
            }
        } catch (Exception e) {
            log.error("Lỗi EDIT_ITEM [{}]: {}", context.getClientId(), e.getMessage(), e);
            context.sendResponse(new Response<>(ServerEvent.EDIT_ITEM.name(), ServerEvent.ERROR.name(),
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

                context.sendResponse(new Response<>(ServerEvent.APPROVE_ITEM.name(), ServerEvent.SUCCESS.name(),
                        "Đã duyệt và bắt đầu đấu giá", sessionId));

                NotificationService.getInstance().onItemApproved(
                        session.getSellerAccountName(), session.getName());

                EventBroadcaster.getInstance().broadcastToAll(
                        new Response<>(ServerEvent.RELOAD_AVAILABLE, ServerEvent.NOTIFY,
                                "Có phiên đấu giá mới vừa được duyệt", sessionId));

                log.info("[Server] Admin duyệt phiên ID: {}", sessionId);
            } else {
                context.sendResponse(new Response<>(ServerEvent.APPROVE_ITEM.name(), ServerEvent.ERROR.name(),
                        "Không tìm thấy sản phẩm cần duyệt", null));
            }
        } catch (Exception e) {
            log.error("Lỗi APPROVE_ITEM: {}", e.getMessage(), e);
            context.sendResponse(new Response<>(ServerEvent.APPROVE_ITEM.name(), ServerEvent.ERROR.name(),
                    "Lỗi xử lý duyệt: " + e.getMessage(), null));
        }
    }

    public void rejectItem(String raw) {
        try {
            int sessionId = JsonParser.parseString(raw)
                    .getAsJsonObject().get("payload").getAsInt();

            boolean isRejected = auctionDAO.updateSessionStatus(sessionId, StatusOfAuction.CANCELED);

            if (isRejected) {
                AuctionSession session = auctionDAO.getAuctionById(sessionId);

                context.sendResponse(new Response<>(ServerEvent.REJECT_ITEM.name(), ServerEvent.SUCCESS.name(),
                        "Đã từ chối sản phẩm", sessionId));

                if (session != null) {
                    NotificationService.getInstance().onItemRejected(
                            session.getSellerAccountName(), session.getName(), "Không đạt yêu cầu duyệt");
                }

                EventBroadcaster.getInstance().broadcastToAdmins(
                        new Response<>(ServerEvent.RELOAD_PENDING_ITEMS, ServerEvent.NOTIFY,
                                "Phiên " + sessionId + " vừa bị từ chối", sessionId));

                log.info("[Server] Admin từ chối phiên ID: {}", sessionId);
            } else {
                context.sendResponse(new Response<>(ServerEvent.REJECT_ITEM.name(), ServerEvent.ERROR.name(),
                        "Không thể thực hiện từ chối (Lỗi Database)", null));
            }
        } catch (Exception e) {
            log.error("Lỗi REJECT_ITEM: {}", e.getMessage(), e);
            context.sendResponse(new Response<>(ServerEvent.REJECT_ITEM.name(), ServerEvent.ERROR.name(),
                    "Lỗi xử lý từ chối: " + e.getMessage(), null));
        }
    }

    private Item buildItemFromPayload(JsonObject payload, String seller) {
        ItemFactory factory = ItemFactoryProducer.getFactory(payload.get("type").getAsString());
        return factory.createItem(
                seller,
                payload.get("name").getAsString(),
                payload.get("description").getAsString(),
                payload.get("imageURL").getAsString()
        );
    }

    private AuctionSession buildSessionFromPayload(JsonObject payload, Item item) {
        AuctionSession session = new AuctionSession(
                LocalDateTime.parse(payload.get("startTime").getAsString()),
                LocalDateTime.parse(payload.get("endTime").getAsString()),
                payload.get("currentPrice").getAsDouble(),
                payload.get("bidIncrease").getAsDouble(),
                item
        );
        session.setBuyNowPrice(payload.has("buyNowPrice") && !payload.get("buyNowPrice").isJsonNull()
                ? payload.get("buyNowPrice").getAsDouble() : 0.0);
        return session;
    }
}
