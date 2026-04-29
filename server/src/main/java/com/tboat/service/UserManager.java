package com.tboat.service;

import com.tboat.dao.UserDAO;
import com.tboat.models.User;
import com.tboat.socket.ClientHandler;
import com.tboat.utils.ResponseCode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager {
    private final UserDAO userDAO = new UserDAO();
    private static final Map<String, ClientHandler> onlineUsers = new ConcurrentHashMap<>();

    // Thêm cơ chế Singleton
    private static UserManager instance;
    private UserManager() {} // Khóa hàm khởi tạo

    public static synchronized UserManager getInstance() {
        if (instance == null) instance = new UserManager();
        return instance;
    }

    public ResponseCode register(String account, String password, String nickname) {
        return userDAO.addUser(account, password, nickname);
    }

    public ResponseCode login(String account, String password, ClientHandler handler) {
        User user = userDAO.getUser(account);
        if (user == null) {
            return ResponseCode.NOT_FOUND;
        }

        // KIỂM TRA: Nếu user đã có trong Map onlineUsers, không cho login nữa
        if (onlineUsers.containsKey(account)) {
            System.out.println("[UserManager]: Từ chối login - User " + account + " đang online.");
            return ResponseCode.ALREADY_LOGGED_IN;
        }

        if (user.getPassword().equals(password)) {
            onlineUsers.put(account, handler);
            System.out.println("[UserManager]: User " + account + " is now ONLINE.");
            return ResponseCode.SUCCESS;
        } else {
            return ResponseCode.WRONG_PASSWORD;
        }
    }

    public void logout(String account) {
        if (account != null) {
            onlineUsers.remove(account);
            System.out.println("[UserManager]: User " + account + " logged out.");
        }
    }

    public static ClientHandler getHandler(String account) {
        return onlineUsers.get(account);
    }

    public static Map<String, ClientHandler> getOnlineUsers() {
        return onlineUsers;
    }
}