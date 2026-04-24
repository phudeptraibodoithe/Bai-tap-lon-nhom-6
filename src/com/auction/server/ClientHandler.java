package com.auction.server;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private AuctionRoom currentRoom;
    private String clientId;

    //this la chinh doi tuong ClientHandler dang xu li
    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.clientId = "Bidder-" + this.hashCode();
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
