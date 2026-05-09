package services;

import models.User;
import server.src.resoures.

public class UserManager {

    UserDAO userDAO = new UserDAO();

    // Phần đăng ký
    public void register(User newUser) {
        // Nếu ĐÃ tồn tại -> Không cho đăng ký
        if (userDAO.getUser(newUser.getAccountName()) != null) {
            System.out.println("Error: Account already exists!");
            return;
        }
        userDAO.addUser(newUser.getAccountName(), newUser.getPassword(), newUser.getNickname());
        System.out.println("Registration successful!");
    }

    // Phần đăng nhập
    public User login(String account, String password) {

        User user = userDAO.getUser(account);
        if (user.getPassword().equals(password)) {
            System.out.println("Login successful!");
            return user;
        } else {
            System.out.println("Error: Incorrect password!");
            return null;
        }
    }
}