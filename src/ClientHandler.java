import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class ClientHandler implements Runnable {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String giaDauGia = in.readLine(); // Đợi khách hàng gõ gì đó từ Terminal rồi nhấn Enter
            System.out.println("Khach vua tra gia: " + giaDauGia);
            out.println("Chao mung! Ban dang duoc xu ly rieng boi luong: " + Thread.currentThread().getName());

//            Thread.sleep(10000); // Ngủ 10s để test thử việc kết nối nhiều máy

            out.println("Xong viec roi, tam biet!");
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}