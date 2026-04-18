import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {
    public static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    public static int currentPrice = 0;
    public static String winner = "";

    public static void main(String[] args) {
        int port = 8888; // Cổng kết nối tự chọn

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server dang khoi tao tren port " + port + "...");

            // Thông báo trước khi lệnh .accept() được gọi
            System.out.println("Server dang doi ket noi tu Client...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Khach moi ket noi " +clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler);
                new Thread(handler).start();
                broadcast("Nguoi choi moi: " + handler.hashCode() +" vua tham gia.");
            }
        } catch (IOException e) {
            System.err.println("Loi Server: " + e.getMessage());
        }
    }

    public static void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendmessage(message);
        }
    }

    public static synchronized boolean updatePrice(int newPrice) {
        if (newPrice > currentPrice) {
            currentPrice = newPrice;
            return true;
        }
        else return false;
    }
}