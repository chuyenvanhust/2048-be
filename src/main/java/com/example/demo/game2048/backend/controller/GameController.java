package com.example.demo.game2048.backend.controller;

import com.example.demo.game2048.backend.dto.AiConfigDTO;
import com.example.demo.game2048.backend.dto.BatchResultDTO;
import com.example.demo.game2048.backend.dto.GameStateDTO;
import com.example.demo.game2048.backend.entity.GameRecord;
import com.example.demo.game2048.backend.repository.GameRecordRepository;
import com.example.demo.game2048.backend.service.GameService;
import com.example.demo.game2048.backend.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = {"http://localhost:5173", "https://two048-fe-1.onrender.com"})
public class GameController {

    @Autowired
    private GameService gameService;

    @Autowired
    private GameRecordRepository gameRecordRepository;

    @Autowired
    private SessionService sessionService;

    // ====== SESSION MANAGEMENT ======

    @PostMapping("/session/create")
    public ResponseEntity<Map<String, String>> createSession() {
        String sessionId = sessionService.createSession();
        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "message", "Session created successfully"
        ));
    }

    @GetMapping("/session/validate")
    public ResponseEntity<Map<String, Object>> validateSession(
            @RequestHeader("X-Session-Id") String sessionId) {
        boolean valid = sessionService.isValidSession(sessionId);
        return ResponseEntity.ok(Map.of(
                "valid", valid,
                "sessionId", sessionId
        ));
    }

    @GetMapping("/session/stats")
    public ResponseEntity<Map<String, Object>> getSessionStats() {
        return ResponseEntity.ok(sessionService.getSessionStats());
    }

    // ====== BOARD-SPECIFIC ENDPOINTS (Session-aware) ======

    @GetMapping("/board/{boardId}/status")
    public ResponseEntity<GameStateDTO> getBoardState(
            @RequestHeader("X-Session-Id") String sessionId,
            @PathVariable int boardId) {
        try {
            return ResponseEntity.ok(gameService.getGameState(sessionId, boardId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/board/{boardId}/new")
    public ResponseEntity<GameStateDTO> newGameForBoard(
            @RequestHeader("X-Session-Id") String sessionId,
            @PathVariable int boardId,
            @RequestParam(required = false, defaultValue = "Guest") String username) {
        try {
            gameService.startNewGame(sessionId, username, boardId);
            return ResponseEntity.ok(gameService.getGameState(sessionId, boardId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/board/{boardId}/move/{direction}")
    public ResponseEntity<GameStateDTO> moveBoard(
            @RequestHeader("X-Session-Id") String sessionId,
            @PathVariable int boardId,
            @PathVariable String direction) {
        try {
            gameService.move(sessionId, direction.toUpperCase(), boardId);
            return ResponseEntity.ok(gameService.getGameState(sessionId, boardId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/board/{boardId}/ai-move")
    public ResponseEntity<GameStateDTO> aiMoveBoard(
            @RequestHeader("X-Session-Id") String sessionId,
            @PathVariable int boardId,
            @RequestParam String algorithm) {
        try {
            gameService.markAiStarted(sessionId, boardId);
            String bestMove = gameService.getAiMove(sessionId, boardId, algorithm);
            if (bestMove != null) {
                gameService.move(sessionId, bestMove, boardId);
            }
            return ResponseEntity.ok(gameService.getGameState(sessionId, boardId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).build();
        }
    }

    @GetMapping("/board/{boardId}/hint")
    public ResponseEntity<GameStateDTO> getHintForBoard(
            @RequestHeader("X-Session-Id") String sessionId,
            @PathVariable int boardId,
            @RequestParam String algorithm) {
        try {
            return ResponseEntity.ok(gameService.getHint(sessionId, boardId, algorithm));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PutMapping("/board/{boardId}/ai-config")
    public ResponseEntity<Map<String, Object>> configureBoardAI(
            @RequestHeader("X-Session-Id") String sessionId,
            @PathVariable int boardId,
            @RequestBody AiConfigDTO config) {
        try {
            gameService.setAiDepthForBoard(sessionId, boardId, config.getDepth());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "boardId", boardId,
                    "depth", config.getDepth(),
                    "message", "AI depth configured for board " + boardId
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PutMapping("/board/{boardId}/username")
    public ResponseEntity<Map<String, Object>> updateBoardUsername(
            @RequestHeader("X-Session-Id") String sessionId,
            @PathVariable int boardId,
            @RequestBody Map<String, String> body) {
        try {
            String username = body.get("username");
            gameService.updateBoardUsername(sessionId, boardId, username);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "boardId", boardId,
                    "username", username
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).build();
        }
    }

    @DeleteMapping("/board/{boardId}")
    public ResponseEntity<Map<String, Object>> clearBoard(
            @RequestHeader("X-Session-Id") String sessionId,
            @PathVariable int boardId) {
        try {
            gameService.clearBoard(sessionId, boardId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Board " + boardId + " cleared"
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).build();
        }
    }

    // ====== BATCH OPERATIONS ======

    @PostMapping("/board/{boardId}/batch-run")
    public ResponseEntity<BatchResultDTO> runBatchForBoard(
            @RequestHeader("X-Session-Id") String sessionId,
            @PathVariable int boardId,
            @RequestParam(defaultValue = "10") int count,
            @RequestParam String algorithm,
            @RequestParam(required = false, defaultValue = "BatchUser") String username) {
        try {
            BatchResultDTO results = gameService.runBatchGames(sessionId, count, username, algorithm, boardId);
            return ResponseEntity.ok(results);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).build();
        }
    }

    @DeleteMapping("/session/boards/all")
    public ResponseEntity<Map<String, Object>> clearAllSessionBoards(
            @RequestHeader("X-Session-Id") String sessionId) {
        try {
            gameService.clearSessionBoards(sessionId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "All session boards cleared"
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).build();
        }
    }

    // ====== LEADERBOARD (No session required) ======

    @GetMapping("/leaderboard")
    public ResponseEntity<List<GameRecord>> getLeaderboard(
            @RequestParam(required = false) String gameMode) {
        List<GameRecord> records;
        if (gameMode != null && !gameMode.isEmpty()) {
            records = gameRecordRepository.findTopByGameMode(gameMode);
        } else {
            records = gameRecordRepository.findTopAllGames();
        }
        return ResponseEntity.ok(records);
    }

    @GetMapping("/leaderboard/manual")
    public ResponseEntity<List<GameRecord>> getManualLeaderboard() {
        return ResponseEntity.ok(gameRecordRepository.findTopByGameMode("SINGLE_MANUAL"));
    }

    @GetMapping("/leaderboard/ai")
    public ResponseEntity<List<GameRecord>> getAiLeaderboard() {
        return ResponseEntity.ok(gameRecordRepository.findTopByGameMode("SINGLE_AI"));
    }

    @GetMapping("/leaderboard/batch")
    public ResponseEntity<List<GameRecord>> getBatchLeaderboard() {
        return ResponseEntity.ok(gameRecordRepository.findTopByGameMode("BATCH"));
    }

    @GetMapping("/leaderboard/user/{username}")
    public ResponseEntity<List<GameRecord>> getUserRecords(@PathVariable String username) {
        return ResponseEntity.ok(gameRecordRepository.findByUsernameOrderByScoreDesc(username));
    }

    // ====== STATISTICS (No session required) ======

    @GetMapping("/stats/global")
    public ResponseEntity<List<Map<String, Object>>> getGlobalStats() {
        return ResponseEntity.ok(gameRecordRepository.getGlobalMaxTileStats());
    }

    @GetMapping("/stats/by-mode")
    public ResponseEntity<List<Map<String, Object>>> getStatsByMode(
            @RequestParam(required = false) String gameMode) {
        if (gameMode != null && !gameMode.isEmpty()) {
            return ResponseEntity.ok(gameRecordRepository.getMaxTileStatsByMode(gameMode));
        }
        return ResponseEntity.ok(gameRecordRepository.getGlobalMaxTileStats());
    }

    @GetMapping("/stats/by-category")
    public ResponseEntity<Map<String, Object>> getStatsByCategory(
            @RequestParam String category) {
        List<GameRecord> records = gameRecordRepository.findByCategory(category);

        Map<String, Object> stats = new HashMap<>();
        stats.put("category", category);
        stats.put("totalGames", records.size());

        if (!records.isEmpty()) {
            Map<Integer, Integer> distribution = new HashMap<>();
            for (GameRecord r : records) {
                distribution.merge(r.getMaxTile(), 1, Integer::sum);
            }
            stats.put("distribution", distribution);

            List<Integer> maxTiles = records.stream().map(GameRecord::getMaxTile).toList();
            double mean = maxTiles.stream().mapToDouble(Integer::doubleValue).average().orElse(0);

            double variance = maxTiles.stream()
                    .mapToDouble(v -> Math.pow(v - mean, 2))
                    .average()
                    .orElse(0);

            int mode = maxTiles.stream()
                    .collect(HashMap<Integer, Integer>::new,
                            (m, v) -> m.merge(v, 1, Integer::sum),
                            HashMap::putAll)
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(0);

            stats.put("mean", mean);
            stats.put("variance", variance);
            stats.put("stdDev", Math.sqrt(variance));
            stats.put("mode", mode);
            stats.put("max", Collections.max(maxTiles));
            stats.put("min", Collections.min(maxTiles));
        }

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/user/{username}")
    public ResponseEntity<Map<String, Object>> getUserStats(@PathVariable String username) {
        List<GameRecord> records = gameRecordRepository.findByUsername(username);

        int totalGames = records.size();
        int totalScore = records.stream().mapToInt(GameRecord::getScore).sum();
        int maxTile = records.stream().mapToInt(GameRecord::getMaxTile).max().orElse(0);
        int totalHints = records.stream().mapToInt(GameRecord::getHintsUsed).sum();

        return ResponseEntity.ok(Map.of(
                "username", username,
                "totalGames", totalGames,
                "totalScore", totalScore,
                "maxTile", maxTile,
                "totalHintsUsed", totalHints,
                "avgScore", totalGames > 0 ? totalScore / totalGames : 0
        ));
    }

    @GetMapping("/stats/mode-distribution")
    public ResponseEntity<List<Map<String, Object>>> getModeDistribution() {
        return ResponseEntity.ok(gameRecordRepository.getGameModeStats());
    }

    @GetMapping("/stats/top-players")
    public ResponseEntity<List<Map<String, Object>>> getTopPlayers() {
        return ResponseEntity.ok(gameRecordRepository.getTopPlayers());
    }

    @GetMapping("/stats/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(gameRecordRepository.getAllCategories());
    }
}