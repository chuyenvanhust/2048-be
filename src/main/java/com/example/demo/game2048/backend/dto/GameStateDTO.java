// Đường dẫn: src/main/java/com/example/demo/game2048/backend/dto/GameStateDTO.java

package com.example.demo.game2048.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data

public class GameStateDTO {

    private int[][] board;
    private int score;
    private boolean gameOver;
    private String gameMode;
    private String username;

    public GameStateDTO(
            int[][] board,
            int score,
            boolean gameOver,
            String gameMode,
            String username
    ) {
        this.board = board;
        this.score = score;
        this.gameOver = gameOver;
        this.gameMode = gameMode;
        this.username = username;
    }

    public GameStateDTO() {}
}
