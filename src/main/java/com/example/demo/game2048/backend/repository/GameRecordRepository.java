package com.example.demo.game2048.backend.repository;

import com.example.demo.game2048.backend.entity.GameRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface GameRecordRepository extends JpaRepository<GameRecord, Long> {

    // BXH tổng hợp (tất cả mode) - Top 10, xếp theo maxTile giảm dần, sau đó score giảm dần
    @Query(value = "SELECT * FROM game_records ORDER BY max_tile DESC, score DESC LIMIT 10", nativeQuery = true)
    List<GameRecord> findTopAllGames();

    // BXH theo gameMode cụ thể - Top 10
    @Query(value = "SELECT * FROM game_records WHERE game_mode = :gameMode ORDER BY max_tile DESC, score DESC LIMIT 10", nativeQuery = true)
    List<GameRecord> findTopByGameMode(@Param("gameMode") String gameMode);

    // Lấy tất cả records của một user, xếp theo maxTile và score giảm dần
    @Query(value = "SELECT * FROM game_records WHERE username = :username ORDER BY max_tile DESC, score DESC", nativeQuery = true)
    List<GameRecord> findByUsernameOrderByScoreDesc(@Param("username") String username);

    // Lấy tất cả records của một user (không sort)
    List<GameRecord> findByUsername(String username);

    // Thống kê tổng hợp Max-Tile (tất cả mode)
    @Query("SELECT r.maxTile as tile, COUNT(r) as count FROM GameRecord r GROUP BY r.maxTile ORDER BY r.maxTile DESC")
    List<Map<String, Object>> getGlobalMaxTileStats();

    // Thống kê Max-Tile theo mode
    @Query("SELECT r.maxTile as tile, COUNT(r) as count FROM GameRecord r WHERE r.gameMode = :gameMode GROUP BY r.maxTile ORDER BY r.maxTile DESC")
    List<Map<String, Object>> getMaxTileStatsByMode(@Param("gameMode") String gameMode);

    // Thống kê số lượng games theo mode
    @Query("SELECT r.gameMode as mode, COUNT(r) as count FROM GameRecord r GROUP BY r.gameMode")
    List<Map<String, Object>> getGameModeStats();

    // Top players (dựa trên maxTile cao nhất)
    @Query(value = "SELECT username, MAX(max_tile) as maxTile, MAX(score) as bestScore, COUNT(*) as gamesPlayed " +
            "FROM game_records GROUP BY username ORDER BY MAX(max_tile) DESC, MAX(score) DESC LIMIT 10", nativeQuery = true)
    List<Map<String, Object>> getTopPlayers();

    // NEW: Find by category (MANUAL, EXPECTIMAX, GREEDY, BFS, DFS, IDS, MINIMAX)
    // This will match both SINGLE_<algo> and BATCH_<algo>
    @Query(value = "SELECT * FROM game_records WHERE " +
            "CASE " +
            "  WHEN :category = 'MANUAL' THEN game_mode = 'SINGLE_MANUAL' " +
            "  ELSE game_mode LIKE CONCAT('%_', :category) " +
            "END", nativeQuery = true)
    List<GameRecord> findByCategory(@Param("category") String category);

    // NEW: Get all unique categories
    @Query(value = "SELECT DISTINCT " +
            "CASE " +
            "  WHEN game_mode = 'SINGLE_MANUAL' THEN 'MANUAL' " +
            "  WHEN game_mode LIKE 'SINGLE_%' THEN SUBSTRING(game_mode, 8) " +
            "  WHEN game_mode LIKE 'BATCH_%' THEN SUBSTRING(game_mode, 7) " +
            "  ELSE game_mode " +
            "END as category " +
            "FROM game_records " +
            "ORDER BY category", nativeQuery = true)
    List<String> getAllCategories();
}