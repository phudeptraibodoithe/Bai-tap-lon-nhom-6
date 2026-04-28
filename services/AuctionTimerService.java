package services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import models.AuctionSession;

public class AuctionTimerService {
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    // Hàm này được gọi khi Phiên đấu giá chuyển sang trạng thái ONGOING

    public void scheduleAuctionClose(AuctionSession session) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = session.getEndTime(); // Giả sử bạn có trường endTime

        // Tính toán khoảng thời gian (giây) từ hiện tại cho đến lúc kết thúc
        long delaySeconds = Duration.between(now, endTime).getSeconds();

        if (delaySeconds <= 0) {
            // Nếu thời gian đã trôi qua, đóng phiên ngay lập tức
            closeAuction(session);
            return;
        }

        // Lên lịch cho tác vụ đóng phiên chạy sau 'delaySeconds'
        scheduler.schedule(() -> {
            closeAuction(session);
        }, delaySeconds, TimeUnit.SECONDS);
        
        System.out.println("Đã lên lịch đóng phiên " + session.getId() + " sau " + delaySeconds + " giây.");
    }

    /**
     * Logic chốt phiên (Cần synchronized để tránh xung đột với lúc người dùng đang đặt giá)
     */
    private void closeAuction(AuctionSession session) {
        // Khóa object session lại để chắc chắn không ai bid được trong lúc đang chốt
        synchronized (session) { 
            if ("RUNNING".equals(session.getStatus())) {
                session.setStatusOfAuction(ENDED);
                System.out.println("--- PHIÊN " + session.getId() + " ĐÃ KẾT THÚC ---");
                
                if (session.getHighestBidderAccount() != null) {
                    System.out.println("Người thắng: " + session.getHighestBidderAccount() + " với giá: " + session.getCurrentPrice());
                    // TODO: Gọi hàm lưu History vào Database
                } else {
                    System.out.println("Phiên kết thúc không có ai trả giá.");
                }
                
                // TODO: Gọi socket gửi thông báo Broadcast cho tất cả Client biết phiên đã đóng
            }
        }
    }
}