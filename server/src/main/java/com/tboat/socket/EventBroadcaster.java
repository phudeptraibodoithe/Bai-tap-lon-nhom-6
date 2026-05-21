package com.tboat.socket;

import com.tboat.models.network.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gửi một event của server tới nhiều client đang online.
 * Client thường và client admin được lưu riêng vì một số event reload chỉ nên
 * gửi tới màn hình quản trị.
 */
public class EventBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(EventBroadcaster.class);

    private static volatile EventBroadcaster instance;

    private final Set<ClientSession> allClients   = ConcurrentHashMap.newKeySet();

    private final Set<ClientSession> adminClients = ConcurrentHashMap.newKeySet();

    private EventBroadcaster() {}

    public static EventBroadcaster getInstance() {
        if (instance == null) {
            synchronized (EventBroadcaster.class) {
                if (instance == null) instance = new EventBroadcaster();
            }
        }
        return instance;
    }

    public void register(ClientSession client) {
        allClients.add(client);
        log.debug("[EventBroadcaster] Registered client: {}", client.getClientId());
    }

    public void unregister(ClientSession client) {
        allClients.remove(client);
        adminClients.remove(client);
        log.debug("[EventBroadcaster] Unregistered client: {}", client.getClientId());
    }

    public void registerAdmin(ClientSession client) {
        adminClients.add(client);
        log.debug("[EventBroadcaster] Registered admin/manager: {}", client.getClientId());
    }

    public void broadcastToAll(Response<?> response) {
        int count = 0;
        for (ClientSession client : allClients) {
            try {
                client.sendResponse(response);
                count++;
            } catch (Exception e) {
                log.warn("[EventBroadcaster] Không thể gửi đến client {}: {}",
                        client.getClientId(), e.getMessage());
            }
        }
        log.info("[EventBroadcaster] broadcastToAll '{}' → {} clients", response.getType(), count);
    }

    public void broadcastToAdmins(Response<?> response) {
        int count = 0;
        for (ClientSession client : adminClients) {
            try {
                client.sendResponse(response);
                count++;
            } catch (Exception e) {
                log.warn("[EventBroadcaster] Không thể gửi đến admin {}: {}",
                        client.getClientId(), e.getMessage());
            }
        }
        log.info("[EventBroadcaster] broadcastToAdmins '{}' → {} admins", response.getType(), count);
    }

    public void broadcastToClient(String accountName, Response<?> response) {
        for (ClientSession client : allClients) {
            if (accountName.equals(client.getClientId())) {
                try {
                    client.sendResponse(response);
                    log.info("[EventBroadcaster] Sent '{}' to client: {}", response.getType(), accountName);
                } catch (Exception e) {
                    log.warn("[EventBroadcaster] Không thể gửi đến {}: {}", accountName, e.getMessage());
                }
                return;
            }
        }
        log.debug("[EventBroadcaster] Client {} không online, bỏ qua broadcast.", accountName);
    }
}
