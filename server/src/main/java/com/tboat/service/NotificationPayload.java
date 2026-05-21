package com.tboat.service;

import com.google.gson.JsonObject;
import com.tboat.models.network.ServerEvent;

record NotificationPayload(
        ServerEvent type,
        String title,
        String subtitle,
        String avatarText,
        String avatarColor
) {
    JsonObject toJson() {
        JsonObject payload = new JsonObject();
        payload.addProperty("notifType", type.name());
        payload.addProperty("title", title);
        payload.addProperty("subtitle", subtitle);
        payload.addProperty("avatarText", avatarText);
        payload.addProperty("avatarColor", avatarColor);
        return payload;
    }
}
