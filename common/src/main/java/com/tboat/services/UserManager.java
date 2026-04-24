package services;

import com.tboat.models.User;

import java.util.HashMap;
import java.util.Map;

public class UserManager {
    private Map<String, User> userMap = new HashMap<>();

    // Kiểm tra accountName/nickname đã tồn tại chưa
    public boolean isUserExists(String account) {
        return userMap.containsKey(account);
    }

    // Phần đăng ký
    public void register(User newUser) {
        // Nếu ĐÃ tồn tại -> Không cho đăng ký
        if (isUserExists(newUser.getAccountName())) {
            System.out.println("Error: Account already exists!");
            return;
        }
        userMap.put(newUser.getAccountName(), newUser);
        System.out.println("Registration successful!");
    }

    // Phần đăng nhập
    public User login(String account, String password) {
        if (!isUserExists(account)) {
            System.out.println("Account does not exist on the system!");
            return null;
        }

        User user = userMap.get(account);
        if (user.getPassword().equals(password)) {
            System.out.println("Login successful!");
            return user;
        } else {
            System.out.println("Error: Incorrect password!");
            return null;
        }
    }
}