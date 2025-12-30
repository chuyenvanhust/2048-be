package com.example.demo.game2048.backend.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {

    // sessionId -> Set of boardIds for that session
    private final Map<String, Set<String>> sessionBoards = new ConcurrentHashMap<>();

    // Track last activity time for cleanup
    private final Map<String, Long> sessionLastActivity = new ConcurrentHashMap<>();

    // Session timeout: 30 minutes
    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000;

    /**
     * Generate a unique session ID
     */
    public String createSession() {
        String sessionId = UUID.randomUUID().toString();
        sessionBoards.put(sessionId, ConcurrentHashMap.newKeySet());
        sessionLastActivity.put(sessionId, System.currentTimeMillis());
        return sessionId;
    }

    /**
     * Validate if a session exists and is active
     */
    public boolean isValidSession(String sessionId) {
        if (sessionId == null || !sessionBoards.containsKey(sessionId)) {
            return false;
        }

        Long lastActivity = sessionLastActivity.get(sessionId);
        if (lastActivity == null) {
            return false;
        }

        // Check if session has timed out
        if (System.currentTimeMillis() - lastActivity > SESSION_TIMEOUT_MS) {
            cleanupSession(sessionId);
            return false;
        }

        return true;
    }

    /**
     * Update session activity timestamp
     */
    public void touchSession(String sessionId) {
        if (sessionBoards.containsKey(sessionId)) {
            sessionLastActivity.put(sessionId, System.currentTimeMillis());
        }
    }

    /**
     * Register a board for a session
     */
    public void registerBoard(String sessionId, String boardKey) {
        if (!sessionBoards.containsKey(sessionId)) {
            sessionBoards.put(sessionId, ConcurrentHashMap.newKeySet());
        }
        sessionBoards.get(sessionId).add(boardKey);
        touchSession(sessionId);
    }

    /**
     * Get all boards for a session
     */
    public Set<String> getSessionBoards(String sessionId) {
        return sessionBoards.getOrDefault(sessionId, Collections.emptySet());
    }

    /**
     * Clean up a specific session
     */
    public void cleanupSession(String sessionId) {
        sessionBoards.remove(sessionId);
        sessionLastActivity.remove(sessionId);
    }

    /**
     * Clean up expired sessions (can be called periodically)
     */
    public int cleanupExpiredSessions() {
        int cleanedCount = 0;
        long currentTime = System.currentTimeMillis();

        List<String> expiredSessions = new ArrayList<>();

        for (Map.Entry<String, Long> entry : sessionLastActivity.entrySet()) {
            if (currentTime - entry.getValue() > SESSION_TIMEOUT_MS) {
                expiredSessions.add(entry.getKey());
            }
        }

        for (String sessionId : expiredSessions) {
            cleanupSession(sessionId);
            cleanedCount++;
        }

        return cleanedCount;
    }

    /**
     * Get total number of active sessions
     */
    public int getActiveSessionCount() {
        return sessionBoards.size();
    }

    /**
     * Get statistics about sessions
     */
    public Map<String, Object> getSessionStats() {
        cleanupExpiredSessions();

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeSessions", sessionBoards.size());
        stats.put("totalBoards", sessionBoards.values().stream()
                .mapToInt(Set::size).sum());
        stats.put("averageBoardsPerSession",
                sessionBoards.isEmpty() ? 0 :
                        sessionBoards.values().stream().mapToInt(Set::size).average().orElse(0));

        return stats;
    }
}