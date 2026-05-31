package com.tboat.socket.handler;

import com.google.gson.reflect.TypeToken;
import com.tboat.dao.UserDAO;
import com.tboat.models.network.Request;
import com.tboat.models.network.Response;
import com.tboat.models.core.User;
import com.tboat.models.network.ServerEvent;
import com.tboat.service.NotificationService;
import com.tboat.service.UserManager;
import com.tboat.socket.ClientSession;
import com.tboat.socket.EventBroadcaster;
import com.tboat.utils.ResponseCode;

import java.lang.reflect.Type;

public class AuthCommandHandler {

    private final ClientSession context;
    private final UserDAO       userDAO     = new UserDAO();
    private final UserManager   userManager = UserManager.getInstance();

    public AuthCommandHandler(ClientSession context) { this.context = context; }

    /*
     * Đăng nhập lưu clientId vào context. Sau đó các handler khác có thể dùng
     * context.getClientId() thay vì đọc accountName từ request.
     */
    public void login(String raw) {
        Type type = new TypeToken<Request<User>>(){}.getType();
        Request<User> request = ClientSession.gson().fromJson(raw, type);
        User loginUser = request.getPayload();

        ResponseCode result = userManager.login(loginUser.getAccountName(), loginUser.getPassword(), context);

        if (result == ResponseCode.SUCCESS) {
            context.setClientId(loginUser.getAccountName());
            NotificationService.getInstance().register(context.getClientId(), context);
            if ("admin".equalsIgnoreCase(loginUser.getAccountName())) {
                EventBroadcaster.getInstance().registerAdmin(context);
            }
            User fullUser = userDAO.getUser(context.getClientId());
            context.sendResponse(new Response<>(ServerEvent.LOGIN.name(), ServerEvent.SUCCESS.name(),
                    "Đăng nhập thành công", fullUser));
        } else {
            context.sendResponse(new Response<>(ServerEvent.LOGIN.name(), ServerEvent.FAILED.name(),
                    result.name(), null));
        }
    }

    public void register(String raw) {
        Type type = new TypeToken<Request<User>>(){}.getType();
        Request<User> request = ClientSession.gson().fromJson(raw, type);
        User user = request.getPayload();

        ResponseCode result = userDAO.addUser(
                user.getAccountName(), user.getPassword(), user.getNickname(),
                user.getEmail(), user.getPhone());

        context.sendResponse(new Response<>(
                ServerEvent.REGISTER.name(),
                result == ResponseCode.SUCCESS ? ServerEvent.SUCCESS.name() : ServerEvent.FAILED.name(),
                result == ResponseCode.EXISTED ? "Tài khoản đã tồn tại!" : result.name(),
                null));
    }

    public void logout() {
        String username = context.getClientId();
        userManager.logout(username);
        NotificationService.getInstance().unregister(username);
        context.setClientId("Guest");
        if (context.getCurrentRoom() != null)
            context.getCurrentRoom().removeSubscriber(context);
        context.setCurrentRoom(null);
        context.sendResponse(new Response<>(ServerEvent.LOGOUT.name(), ServerEvent.SUCCESS.name(),
                "Đã đăng xuất", null));
    }
}
