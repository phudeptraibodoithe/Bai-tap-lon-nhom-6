package com.auction.client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class AuctionClient {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Cho phép nhập IP để linh hoạt khi ghép nối nhóm
        System.out.print("Nhap IP Server (mac dinh 192.168.1.16): ");
        String serverIp = scanner.nextLine().trim();
        if (serverIp.isEmpty()) serverIp = "192.168.1.16";

        try (Socket socket = new Socket(serverIp, 8888);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("KET NOI THANH CONG DEN " + serverIp + " ....");

            // Khai báo biến volatile để kiểm soát trạng thái kết nối giữa các luồng
            Thread listenerThread = new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        System.out.println("\n[SERVER]: " + msg);
                        System.out.print("> ");
                    }
                } catch (IOException e) {
                    System.err.println("\n[LOI]: Mat ket noi voi Server.");
                }
            });
            listenerThread.setDaemon(true); // Luồng này sẽ tự đóng khi luồng chính kết thúc
            listenerThread.start();

            while (true) {
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("EXIT")) break;
                if (!input.isEmpty()) out.println(input);
            }

        } catch (IOException e) {
            System.err.println("LOI: Khong the ket noi. Hay kiem tra IP va dam bao Server da chay.");
        }
    }
}