import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        int port = 8888; // Cổng kết nối tự chọn

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server dang khoi tao tren port " + port + "...");

            // Thông báo trước khi lệnh .accept() được gọi
            System.out.println("Server dang doi ket noi tu Client...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Khach moi ket noi " +clientSocket.getInetAddress());

                Thread t = new Thread(new ClientHandler(clientSocket));
                t.start();
            }
        } catch (IOException e) {
            System.err.println("Loi Server: " + e.getMessage());
        }
    }
}