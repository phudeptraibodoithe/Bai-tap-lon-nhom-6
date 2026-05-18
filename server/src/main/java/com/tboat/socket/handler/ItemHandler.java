package com.tboat.socket.handler;

import com.google.gson.*;
import com.tboat.dao.AuctionSessionDAO;
import com.tboat.dao.ItemDAO;
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

    private final ClientContext     context;
    private final AuctionSessionDAO auctionDAO       = new AuctionSessionDAO();
    private final ItemDAO           itemDAO          = new ItemDAO();
    private final ParticipationDAO  participationDAO = new ParticipationDAO();
    private final SellerService     sellerService    = new SellerService();

    public ItemHandler(ClientContext context) { this.context = context; }

    public void getPendingItems() {
        try {
            var list = auctionDAO.getPendingAuctions();
            context.sendResponse(new Response<>("GET_PENDING_ITEMS", "SUCCESS",
                    "Danh sách chờ duyệt", list != null ? list : new ArrayList<>()));
        } catch (Exception e) {
            log.error("Lỗi GET_PENDING_ITEMS: {}", e.getMessage(), e);
            context.sendResponse(new Response<>("GET_PENDING_ITEMS", "ERROR",
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
            boolean isUpdated = auctionDAO.updateSessionStatus(sessionId, StatusOfAuction.NOT_STARTED);

            if (isUpdated) {
                AuctionSession session = auctionDAO.getAuctionById(sessionId);

                if (session != null) {
                    AuctionTimerService.getInstance().scheduleAuction(session);
                    context.sendResponse(new Response<>("APPROVE_ITEM", "SUCCESS",
                            "Đã duyệt và bắt đầu đấu giá", sessionId));

                    log.info("[Server] Admin duyệt thành công phiên ID: {}", sessionId);
                } else {
                    context.sendResponse(new Response<>("APPROVE_ITEM", "ERROR",
                            "Không tìm thấy sản phẩm sau khi cập nhật", null));
                }
            } else {
                context.sendResponse(new Response<>("APPROVE_ITEM", "ERROR",
                        "Lỗi cập nhật trạng thái duyệt vào Database", null));
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
                context.sendResponse(new Response<>("REJECT_ITEM", "SUCCESS",
                        "Đã từ chối sản phẩm", sessionId));
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
        return new AuctionSession(
                LocalDateTime.parse(p.get("startTime").getAsString()),
                LocalDateTime.parse(p.get("endTime").getAsString()),
                p.get("currentPrice").getAsDouble(),
                p.get("bidIncrease").getAsDouble(),
                item
        );
    }
}