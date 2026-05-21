package com.tboat.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LogConfig {

    private static final Logger log = LoggerFactory.getLogger(LogConfig.class);

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "log-cleanup-thread");
                t.setDaemon(true);
                return t;
            });

    private final String logDirectory;
    private final int retentionMinutes; // ← đổi sang phút

    public LogConfig(String logDirectory, int retentionMinutes) {
        this.logDirectory = logDirectory;
        this.retentionMinutes = retentionMinutes;
    }

    public void start() {
        log.info("LogConfig started - xoá log cũ hơn {} phút", retentionMinutes);

        // Chạy ngay sau 1 phút, sau đó lặp lại mỗi 15 phút
        scheduler.scheduleAtFixedRate(
                this::cleanupOldLogs,
                1,               // delay lần đầu (phút)
                retentionMinutes, // lặp lại sau mỗi 15 phút
                TimeUnit.MINUTES
        );
    }

    public void stop() {
        scheduler.shutdown();
        log.info("LogConfig stopped.");
    }

    private void cleanupOldLogs() {
        Path logPath = Paths.get(logDirectory);

        if (!Files.exists(logPath)) {
            log.warn("Không tìm thấy thư mục log: {}", logDirectory);
            return;
        }

        // Mốc thời gian: xoá file cũ hơn 15 phút
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(retentionMinutes);
        log.info("Bắt đầu dọn log cũ hơn {} phút (trước {})", retentionMinutes, cutoff);

        try {
            long[] stats = {0, 0}; // [số file xoá, tổng bytes]

            Files.walkFileTree(logPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {

                    LocalDateTime lastModified = attrs.lastModifiedTime()
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();

                    String name = file.getFileName().toString();
                    boolean isLog = name.endsWith(".log") || name.endsWith(".log.gz");

                    if (isLog && lastModified.isBefore(cutoff)) {
                        long size = attrs.size();
                        Files.delete(file);
                        stats[0]++;
                        stats[1] += size;
                        log.debug("Đã xoá: {} ({}KB)", name, size / 1024);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException e) {
                    log.error("Không thể truy cập file: {}", file, e);
                    return FileVisitResult.CONTINUE;
                }
            });

            log.info("Dọn xong! Đã xoá {} file, giải phóng {}KB",
                    stats[0], stats[1] / 1024);

        } catch (IOException e) {
            log.error("Lỗi khi dọn log", e);
        }
    }
}