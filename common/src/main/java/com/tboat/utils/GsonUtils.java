package com.tboat.utils;

import com.google.gson.*;
import com.tboat.models.auction.AuctionSession;
import com.tboat.models.item.ElectronicsItem;
import com.tboat.models.item.FashionItem;
import com.tboat.models.item.JewelryItem;
import com.tboat.models.item.OtherItem;

import java.lang.reflect.Type;
import java.time.LocalDateTime;

public class GsonUtils {
    private static Gson instance;
    private GsonUtils() {}

    public static Gson getInstance() {
        if (instance == null) {
            instance = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>)
                            (src, typeOfT, context) -> new JsonPrimitive(src.toString())) // Dùng toString() mặc định
                    .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>)
                            (json, typeOfT, context) -> LocalDateTime.parse(json.getAsString()))

                    .registerTypeAdapter(AuctionSession.class, new JsonDeserializer<AuctionSession>() {
                        @Override
                        public AuctionSession deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                            JsonObject jsonObject = json.getAsJsonObject();

                            String type = jsonObject.has("type") && !jsonObject.get("type").isJsonNull()
                                    ? jsonObject.get("type").getAsString()
                                    : "Khác";

                            switch (type.trim()) {
                                case "Điện tử":
                                    return context.deserialize(json, ElectronicsItem.class);
                                case "Thời trang":
                                    return context.deserialize(json, FashionItem.class);
                                case "Trang sức":
                                    return context.deserialize(json, JewelryItem.class);
                                case "Khác":
                                default:
                                    return context.deserialize(json, OtherItem.class);
                            }
                        }
                    })
                    .create();
        }
        return instance;
    }
}