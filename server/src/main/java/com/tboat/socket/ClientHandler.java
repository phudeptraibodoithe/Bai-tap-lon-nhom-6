package com.tboat.socket;

import com.tboat.dao.HistoryBidDAO;
import com.tboat.dao.UserDAO;
import com.tboat.models.History;
import com.tboat.models.User;
import com.tboat.service.AuctionManager;
import com.tboat.service.AuctionRoom;
import com.tboat.service.UserManager;
import com.tboat.utils.ResponseCode;

import java.io.*;
import java.net.*;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private AuctionRoom currentRoom;
    private String clientId;
    private ResponseCode result;
    private UserDAO userDAO;
    private HistoryBidDAO historyBidDAO;
    private String pass,nick,account;
    private String[] data;
    private UserManager userManager;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.clientId = "Bidder-" + this.hashCode();
        this.userManager = new UserManager();
        this.userDAO = new UserDAO();
        this.historyBidDAO=new HistoryBidDAO();
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
            case "REGISTER":
                data = parts[1].split(" ", 3);
                account = data[0];
                pass = data[1];
                nick = data[2];

                result = userManager.register(account, pass, nick);
                out.println("REG_" + result.name());
                break;

            case "LOGIN":
                data = parts[1].split(" ", 2);
                this.account = data[0];
                this.pass = data[1];

                result = userManager.login(account, pass, this);
                if (result == ResponseCode.SUCCESS) {
                    User user = userDAO.getUser(account);
                    out.println(String.format("LOGIN_SUCCESS|%s|%.0f|%s|%s",
                            user.getNickname(), user.getBalance(),
                            user.getDescription(), user.getAvatarURL()));
                } else {
                    out.println("LOGIN_" + result.name());
                }
                break;

            case "UPDATE_PROFILE":
                // Kiểm tra xem có phần dữ liệu sau lệnh không
                if (parts.length < 2 || parts[1].trim().isEmpty()) {
                    out.println("UPDATE_PROFILE_ERROR|MISSING_DATA");
                    break;
                }
                data = parts[1].split("\\|", -1);
                // Kiểm tra xem có đủ 2 phần (mô tả và ảnh) không
                if (data.length < 2) {
                    out.println("UPDATE_PROFILE_ERROR|INVALID_FORMAT");
                    break;
                }
                String newDesc = data[0];
                String newAvt = data[1];

                User user = userDAO.getUser(this.account);
                if (user != null) {
                    user.setDescription(newDesc);
                    user.setAvatar(newAvt);

                    if (userDAO.updateUser(user)) {
                        out.println("UPDATE_PROFILE_SUCCESS");
                    } else {
                        out.println("UPDATE_PROFILE_ERROR");
                    }
                }
                break;

            case "GET_HISTORY":
                String targetAccount = (parts.length > 1) ? parts[1] : this.account;
                List<History> historyList = historyBidDAO.getHistoryByAccount(targetAccount);

                StringBuilder sb = new StringBuilder("HISTORY_RES");
                for (History h : historyList) {
                    sb.append("|").append(h.getAuctionSessionId())
                            .append(";").append(h.getFinalPrice())
                            .append(";").append(h.getCompletedAt().toString());
                }
                out.println(sb.toString());
                break;

            case "JOIN":
                if (parts.length < 2) return;
                if (currentRoom != null) currentRoom.removeSubscriber(this);
                currentRoom = AuctionManager.getInstance().getRoom(parts[1]);
                currentRoom.addSubscriber(this);
                out.println("DA_VAO_PHONG|" + parts[1]);
                break;

            case "BID":
                if (currentRoom == null || parts.length < 2) {
                    out.println("ERR_JOIN_REQUIRED");
                    return;
                }
                try {
                    double price = Double.parseDouble(parts[1]);
                    if (currentRoom.placeBid(price, this.account)) {
                        currentRoom.broadcast("GIA_MOI|" + price + "|" + this.account);
                    } else {
                        out.println("BID_INVALID|" + currentRoom.getCurrentPrice());
                    }
                } catch (NumberFormatException e) { out.println("ERR_NUMBER_FORMAT"); }
                break;

            default:
                if (currentRoom != null) currentRoom.broadcast(this.account + ": " + input);
                else out.println("ERR_UNKNOWN_COMMAND");
        }
    }

    public void sendmessage(String msg) { if (out != null) out.println(msg); }
}
