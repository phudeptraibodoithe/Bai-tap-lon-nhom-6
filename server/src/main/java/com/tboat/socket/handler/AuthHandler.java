package com.tboat.socket.handler;

import com.google.gson.reflect.TypeToken;
import com.tboat.dao.UserDAO;
import com.tboat.models.Request;
import com.tboat.models.Response;
import com.tboat.models.User;
import com.tboat.service.UserManager;
import com.tboat.socket.ClientContext;
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
            User fullUser = userDAO.getUser(context.getClientId());
            context.sendResponse(new Response<>("LOGIN", "SUCCESS", "Đăng nhập thành công", fullUser));
        } else {
            context.sendResponse(new Response<>("LOGIN", "FAILED", res.name(), null));
        }
    }

    public void register(String raw) {
        Type type = new TypeToken<Request<User>>(){}.getType();
        Request<User> req = ClientContext.gson().fromJson(raw, type);
        User user = req.getPayload();

        ResponseCode res = userManager.register(
                user.getAccountName(), user.getPassword(), user.getNickname());

        context.sendResponse(new Response<>(
                "REGISTER",
                res == ResponseCode.SUCCESS ? "SUCCESS" : "FAILED",
                res.name(), null));
    }

    public void logout() {
        userManager.logout(context.getClientId());
        context.setClientId("Guest");
        if (context.getCurrentRoom() != null)
            context.getCurrentRoom().removeSubscriber(context);
        context.setCurrentRoom(null);
        context.sendResponse(new Response<>("LOGOUT", "SUCCESS", "Đã đăng xuất", null));
    }
}