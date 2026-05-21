package com.tboat.socket.handler;

import com.google.gson.reflect.TypeToken;
import com.tboat.dao.UserDAO;
import com.tboat.models.network.Request;
import com.tboat.models.network.Response;
import com.tboat.models.core.User;
import com.tboat.models.network.ServerEvent;
import com.tboat.service.NotificationService;
import com.tboat.service.UserManager;
import com.tboat.socket.ClientContext;
import com.tboat.socket.GlobalBroadcaster;
import com.tboat.utils.ResponseCode;

import java.lang.reflect.Type;

public class AuthHandler {

    private final ClientContext context;
    private final UserDAO       userDAO     = new UserDAO();
    private final UserManager   userManager = UserManager.getInstance();

    public AuthHandler(ClientContext context) { this.context = context; }

    public void login(String raw) {
        Type type = new TypeToken<Request<User>>(){}.getType();
        Request<User> req = ClientContext.gson().fromJson(raw, type);
        User creds = req.getPayload();

        ResponseCode res = userManager.login(creds.getAccountName(), creds.getPassword(), context);

        if (res == ResponseCode.SUCCESS) {
            context.setClientId(creds.getAccountName());
            NotificationService.getInstance().register(context.getClientId(), context);
            if ("admin".equalsIgnoreCase(creds.getAccountName())) {
                GlobalBroadcaster.getInstance().registerAdmin(context);  // ← thêm dòng này
            }
            User fullUser = userDAO.getUser(context.getClientId());
            context.sendResponse(new Response<>(ServerEvent.LOGIN.name(), ServerEvent.SUCCESS.name(),
                    "Đăng nhập thành công", fullUser));
        } else {
            context.sendResponse(new Response<>(ServerEvent.LOGIN.name(), ServerEvent.FAILED.name(),
                    res.name(), null));
        }
    }

    public void register(String raw) {
        Type type = new TypeToken<Request<User>>(){}.getType();
        Request<User> req = ClientContext.gson().fromJson(raw, type);
        User user = req.getPayload();

        ResponseCode res = userDAO.addUser(
                user.getAccountName(), user.getPassword(), user.getNickname(),
                user.getEmail(), user.getPhone());

        context.sendResponse(new Response<>(
                ServerEvent.REGISTER.name(),
                res == ResponseCode.SUCCESS ? ServerEvent.SUCCESS.name() : ServerEvent.FAILED.name(),
                res == ResponseCode.EXISTED ? "Tài khoản đã tồn tại!" : res.name(),
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
