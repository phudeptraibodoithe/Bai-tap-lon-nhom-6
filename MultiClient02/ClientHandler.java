import javax.imageio.IIOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {

            out.println("Chao mung! Ban dang duoc xu ly rieng boi luong: " + Thread.currentThread().getName());

//            Thread.sleep(10000); // Ngủ 10s để test thử việc kết nối nhiều máy
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                try {
                    int bid = Integer.parseInt(inputLine.trim());

                    if (Main.updatePrice(bid)) {
                        Main.broadcast("Gia moi " + Main.currentPrice +" tu nguoi choi " + this.hashCode());
                    }
                    else {
                        this.sendmessage("Gia dau cua ban dat phai cao hon " + Main.currentPrice);
                    }
                }
                catch (NumberFormatException e) {
                    Main.broadcast("Tu nguoi dung " +this.hashCode()+ ": " + inputLine);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendmessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}