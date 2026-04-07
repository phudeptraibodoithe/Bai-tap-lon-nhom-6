import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SimpleServer {
    public static void main(String[] args) {
        int port = 8888; // Cổng kết nối tự chọn

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server đang khởi tạo trên port " + port + "...");

            // Thông báo trước khi lệnh .accept() được gọi
            System.out.println("Server đang đợi kết nối từ Client...");

            /* Lệnh .accept() là một lệnh "chặn" (blocking). 
               Chương trình sẽ dừng hoàn toàn tại dòng này cho đến khi 
               có một Client nào đó thực hiện kết nối tới.
            */
            Socket socket = serverSocket.accept();

            // Dòng này sẽ CHỈ được in ra sau khi có Client kết nối thành công
            System.out.println("Đã có một Client kết nối thành công!");

            // Gửi một tin nhắn phản hồi cho Client
            //Gửi đi duoi dang Byte
            socket.getOutputStream().write("Hello tu Server!".getBytes());

            // Gui luon cho Client (Neu ko co the java se doi du lieu du nhieu moi gui)
            socket.getOutputStream().flush();

            // Đóng socket sau khi gửi xong
            socket.close();
        } catch (IOException e) {
            System.err.println("Lỗi Server: " + e.getMessage());
        }
    }
}