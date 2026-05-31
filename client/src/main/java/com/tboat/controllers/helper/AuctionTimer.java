package com.tboat.controllers.helper;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.BiConsumer;

public class AuctionTimer {
    private static final int TIMER_TICK_SECONDS = 1;
    private static final int TIME_UNIT_MODULO = 60;

    private Timeline timeline;
    private LocalDateTime endTime;
    private BiConsumer<String, Long> onTick;
    private Runnable onFinish;

    public AuctionTimer(LocalDateTime endTime, BiConsumer<String, Long> onTick, Runnable onFinish) {
        this.endTime = endTime;
        this.onTick = onTick;
        this.onFinish = onFinish;
    }

    public void start() {
        stop();
        timeline = new Timeline(new KeyFrame(Duration.seconds(TIMER_TICK_SECONDS), event -> {
            LocalDateTime now = LocalDateTime.now();

            if (now.isAfter(endTime) || now.isEqual(endTime)) {
                onFinish.run();
                stop();
            } else {
                long hours   = ChronoUnit.HOURS.between(now, endTime);
                long minutes = ChronoUnit.MINUTES.between(now, endTime) % TIME_UNIT_MODULO;
                long seconds = ChronoUnit.SECONDS.between(now, endTime) % TIME_UNIT_MODULO;
                long totalSeconds = ChronoUnit.SECONDS.between(now, endTime);

                onTick.accept(String.format("%02d : %02d : %02d", hours, minutes, seconds), totalSeconds);
            }
        }));

        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    public void stop() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    /**
     * Cập nhật endTime mới và khởi động lại timer ngay lập tức.
     * Gọi khi nhận được "TIME_EXTENDED" hoặc "TIME_UPDATED" từ server.
     */
    public void updateEndTime(LocalDateTime newEndTime) {
        this.endTime = newEndTime;
        start(); // dừng rồi khởi động lại với endTime mới
    }
}
