package com.tboat.models.auction;


import com.tboat.exception.AuctionBusinessException;

import java.time.LocalDateTime;

public class History {
    private int auctionSessionId;
    private String winnerAccountName;
    private double finalPrice;
    private LocalDateTime completedAt;

    public History(){}

    public History(int auctionSessionId, String winnerAccountName, double finalPrice, LocalDateTime completedAt) {
        if (winnerAccountName == null || winnerAccountName.trim().isEmpty()) {
            throw new AuctionBusinessException("ERR_HISTORY_01", "Tên người thắng không được trống");
        }
        if (finalPrice < 0) {
            throw new AuctionBusinessException("ERR_HISTORY_02", "Giá chung cuộc không được âm");
        }
        if (completedAt == null) {
            throw new AuctionBusinessException("ERR_HISTORY_03", "Thời gian hoàn thành không được null");
        }
        this.auctionSessionId = auctionSessionId;
        this.winnerAccountName = winnerAccountName;
        this.finalPrice = finalPrice;
        this.completedAt = completedAt;
    }

    // Hàm truy xuất
    public int getAuctionSessionId() {
        return auctionSessionId;
    }

    public String getWinnerAccountName() {
        return winnerAccountName;
    }

    public double getFinalPrice() {
        return finalPrice;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
