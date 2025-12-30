package com.example.demo.game2048.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "game_records", indexes = {
        @Index(name = "idx_username", columnList = "username"),
        @Index(name = "idx_game_mode", columnList = "gameMode"),
        @Index(name = "idx_max_tile_score", columnList = "maxTile,score")
})
public class GameRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String gameMode; // SINGLE_MANUAL, SINGLE_AI, BATCH

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int maxTile;

    @Column(nullable = false)
    private int hintsUsed;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }
    public String getUsername() {
        return username;
    }

    public String getGameMode() {
        return gameMode;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getMaxTile() {
        return maxTile;
    }

    public void setMaxTile(int maxTile) {
        this.maxTile = maxTile;
    }

    public int getHintsUsed() {
        return hintsUsed;
    }

    public void setHintsUsed(int hintsUsed) {
        this.hintsUsed = hintsUsed;
    }

}