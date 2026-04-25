package com.tboat.socket;

import com.tboat.dao.UserDAO;
import com.tboat.models.User;
import com.tboat.service.AuctionManager;
import com.tboat.service.AuctionRoom;
import com.tboat.service.UserManager;
import com.tboat.utils.ResponseCode;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private AuctionRoom currentRoom;
    private String clientId;
    private UserDAO userDAO;
    private String pass,nick,account;
    private String[] data;
    private UserManager userManager;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.clientId = "Bidder-" + this.hashCode();
        this.userManager = new UserManager();
        this.userDAO = new UserDAO();
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            this.out = new PrintWriter(socket.getOutputStream(), true);
            out.println("Chao mung! Lenh: JOIN <room>, BID <price>, hoac chat truc tiep.");

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                handleCommand(inputLine.trim());
            }
        } catch (IOException e) {
            if (currentRoom != null) currentRoom.removeSubscriber(this);
        }
    }

    private void handleCommand(String input) {
        String[] parts = input.split(" ", 2);
        //duyet trong input gap dau " " la cat nhung chi cat toi da tra ve 2 doi tuong
        String action = parts[0].toUpperCase();

        switch (action) {
            // Bên trong hàm handleCommand của ClientHandler.java
            // Trong file ClientHandler.java (Server)
            case "REGISTER":
                data = parts[1].split(" ",3);
                account = data[0];
                pass    = data[1];
                nick    = data[2];

                // Gọi qua UserManager thay vì UserDAO trực tiếp
                ResponseCode result = userManager.register(account, pass, nick);

                // Gửi phản hồi chính xác về cho Client
                out.println("REG_" + result.name()); // Sẽ gửi về REG_SUCCESS, REG_EXISTED, hoặc REG_ERROR
                break;

            case "LOGIN":
                // Giả sử lệnh gửi lên là: LOGIN account password
                data = parts[1].split(" ");
                account = data[0];
                pass = data[1];

                // Gọi UserManager để kiểm tra (UserManager đã được viết ở các bước trước)
                ResponseCode loginRes = userManager.login(account, pass, this);

                if (loginRes == ResponseCode.SUCCESS) {
                    // Lấy thông tin user để gửi về cho Client lưu trữ
                    User user = userDAO.getUser(account);
                    // Gửi: LOGIN_SUCCESS | Nickname | Balance
                    out.println("LOGIN_SUCCESS|" + user.getNickname() + "|" + user.getBalance());
                } else {
                    // Gửi: LOGIN_WRONG_PASSWORD hoặc LOGIN_NOT_FOUND
                    out.println("LOGIN_" + loginRes.name());
                }
                break;

            case "JOIN":
                if (parts.length < 2) return;

                //logic chuyen phong khi dang o phong A JOIN sang phong B
                //hoac khi 1 nguoi choi ket noi nhung chua vao phong nao da ngat ket noi
                if (currentRoom != null) currentRoom.removeSubscriber(this);

                //khai bao AuctionRoom moi neu chua co va them client vao
                currentRoom = AuctionManager.getInstance().getRoom(parts[1]);
                currentRoom.addSubscriber(this);
                out.println("Đa vao phong đau gia: " + parts[1]);
                break;

            case "BID":
                if (currentRoom == null || parts.length < 2) {
                    out.println("Loi: Hay tham gia phong truoc (JOIN <room>)");
                    return;
                }
                try {
                    double price = Double.parseDouble(parts[1]);
                    if (currentRoom.placeBid(price, clientId)) {
                        currentRoom.broadcast("GIA MOI: " + price + " (Dat boi " + clientId + ")");
                    } else {
                        out.println("Gia khong hop le (Phai cao hơn " + currentRoom.getCurrentPrice() + ")");
                    }
                } catch (NumberFormatException e) { out.println("Gia phai là mot so!"); }
                break;

            default:
                if (currentRoom != null) currentRoom.broadcast(clientId + ": " + input);
                else out.println("Lenh khong ro. Hay JOIN mot phong de bat đau.");
        }
    }

    public void sendmessage(String msg) { if (out != null) out.println(msg); }
}
