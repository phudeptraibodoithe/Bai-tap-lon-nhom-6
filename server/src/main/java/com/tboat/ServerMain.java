package com.tboat;

import com.tboat.socket.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    public static void main(String[] args) {
        int port = 8888;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Auction Server dang chay tren cong " + port + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Ket noi moi tu: " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
