package com.example.demo.game2048.backend.scheduler;

import com.example.demo.game2048.backend.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class SessionCleanupScheduler {

    @Autowired
    private SessionService sessionService;

    /**
     * Run every 10 minutes to clean up expired sessions
     */
    @Scheduled(fixedRate = 600000) // 10 minutes in milliseconds
    public void cleanupExpiredSessions() {
        int cleanedCount = sessionService.cleanupExpiredSessions();

        if (cleanedCount > 0) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            System.out.println("[" + timestamp + "] Session Cleanup: Removed " + cleanedCount + " expired sessions");
        }
    }

    /**
     * Log session statistics every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void logSessionStats() {
        var stats = sessionService.getSessionStats();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        System.out.println("[" + timestamp + "] Session Stats: " +
                "Active Sessions: " + stats.get("activeSessions") +
                ", Total Boards: " + stats.get("totalBoards") +
                ", Avg Boards/Session: " + String.format("%.2f", stats.get("averageBoardsPerSession")));
    }
}