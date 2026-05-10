package com.tboat.utilsclient;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

public class AuctionTimer {
    private Timeline timeline;
    private LocalDateTime endTime;
    private Consumer<String> onTick;
    private Runnable onFinish;

    public AuctionTimer(LocalDateTime endTime, Consumer<String> onTick, Runnable onFinish) {
        this.endTime = endTime;
        this.onTick = onTick;
        this.onFinish = onFinish;
    }

    public void start() {
        stop();
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            LocalDateTime now = LocalDateTime.now();

            if (now.isAfter(endTime) || now.isEqual(endTime)) {
                onFinish.run();
                stop();
            } else {
                long hours = ChronoUnit.HOURS.between(now, endTime);
                long minutes = ChronoUnit.MINUTES.between(now, endTime) % 60;
                long seconds = ChronoUnit.SECONDS.between(now, endTime) % 60;

                String timeString = String.format("%02d : %02d : %02d", hours, minutes, seconds);
                onTick.accept(timeString);
            }
        }));

        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    public void stop() {
        if (timeline != null) {
            timeline.stop();
        }
    }
}