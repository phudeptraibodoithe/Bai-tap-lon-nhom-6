package com.tboat.utilsclient;

import com.google.gson.JsonObject;
import com.tboat.models.auction.AuctionSession;
import com.tboat.models.item.Item;
import com.tboat.models.item.factory.ItemFactory;
import com.tboat.models.item.factory.ItemFactoryProducer;
import com.tboat.models.auction.StatusOfAuction;

import java.time.LocalDateTime;

public class JsonMapperUtils {

    /**
     * Chuyển đổi một JsonObject thành đối tượng AuctionSession
     */
    public static AuctionSession parseAuctionSession(JsonObject dataObj) {
        // Hỗ trợ cả object lồng nhau (item) hoặc object phẳng
        JsonObject itemObj = dataObj.has("item") && dataObj.get("item").isJsonObject()
                ? dataObj.getAsJsonObject("item")
                : dataObj;

        String type          = getStringJson(itemObj, "type", "Khác");
        String name          = getStringJson(itemObj, "name", "Sản phẩm chưa có tên");
        String description   = getStringJson(itemObj, "description", "Không có mô tả");
        String imageURL      = getStringJson(itemObj, "imageURL", "");
        String sellerAccount = getStringJson(itemObj, "sellerAccountName", "N/A");
        String highestBidder = getStringJson(dataObj, "highestBidderAccount", "");

        double currentPrice  = getDoubleJson(dataObj, "currentPrice", 0.0);
        double bidIncrease   = getDoubleJson(dataObj, "bidIncrease", 0.0);
        double buyNowPrice   = getDoubleJson(dataObj, "buyNowPrice", 0.0);

        LocalDateTime startTime = dataObj.has("startTime") ? TimeUtils.parseServerTime(dataObj.get("startTime")) : LocalDateTime.now();
        LocalDateTime endTime   = dataObj.has("endTime")   ? TimeUtils.parseServerTime(dataObj.get("endTime"))   : LocalDateTime.now().plusDays(1);

        // Xử lý chuỗi Base64
        if (imageURL.startsWith("data:image")) {
            imageURL = imageURL.substring(imageURL.indexOf(",") + 1);
        }

        ItemFactory factory = ItemFactoryProducer.getFactory(type);
        Item item = factory.createItem(sellerAccount, name, description, imageURL);

        AuctionSession session = new AuctionSession(startTime, endTime, currentPrice, bidIncrease, item);
        session.setBuyNowPrice(buyNowPrice);
        session.setId(dataObj.has("id") ? dataObj.get("id").getAsInt() : 0);
        session.setHighestBidderAccount(highestBidder);

        String statusStr = getStringJson(dataObj, "statusOfAuction", "ONGOING");
        try {
            session.setStatusOfAuction(StatusOfAuction.valueOf(statusStr));
        } catch (Exception ignored) {
            session.setStatusOfAuction(StatusOfAuction.ONGOING);
        }

        return session;
    }

    // ── Các hàm Helper trích xuất an toàn ──

    public static String getStringJson(JsonObject json, String key, String defaultValue) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : defaultValue;
    }

    public static double getDoubleJson(JsonObject json, String key, double defaultValue) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsDouble() : defaultValue;
    }
}
