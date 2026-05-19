package com.tboat.service;

import com.tboat.dao.UserDAO;
import com.tboat.socket.ClientContext;
import com.tboat.utils.ResponseCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager {
    private final UserDAO userDAO = new UserDAO();
    private static final Map<String, ClientContext> onlineUsers = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(UserManager.class);
    private static volatile UserManager instance;
    private UserManager() {} // Khóa hàm khởi tạo

    public static UserManager getInstance() {
        if (instance == null) {
            synchronized (UserManager.class) {
                if (instance == null) instance = new UserManager();
            }
        }
        return instance;
    }

    public ResponseCode register(String account, String password, String nickname, String email, String phone) {
        return userDAO.addUser(account, password, nickname,email,phone);
    }

    public ResponseCode login(String account, String password, ClientContext handler) {
        if (onlineUsers.containsKey(account)) {
            logger.warn("[UserManager]: Từ chối login - User {} đang online.", account);
            return ResponseCode.ALREADY_LOGGED_IN;
        }
        ResponseCode loginStatus = userDAO.checkLogin(account, password);
        if (loginStatus == ResponseCode.SUCCESS) {
            onlineUsers.put(account, handler);
            logger.info("[UserManager]: User {} is now ONLINE.", account);
        }
        return loginStatus;
    }

    public void logout(String account) {
        if (account != null) {
            onlineUsers.remove(account);
            logger.info("[UserManager]: User {} logged out.", account);
        }
    }

    public static ClientContext getHandler(String account) {
        return onlineUsers.get(account);
    }

    public static Map<String, ClientContext> getOnlineUsers() {
        return onlineUsers;
    }
}