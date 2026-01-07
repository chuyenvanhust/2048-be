package com.example.demo.game2048.backend.dto;

import lombok.Data;

@Data
public class GameStateDTO {
    private int[][] board;
    private int score;
    private boolean gameOver;
    private String suggestedMove;
    private String username;

    public GameStateDTO(int[][] board, int score, boolean gameOver, String suggestedMove, String username) {
        this.board = board;
        this.score = score;
        this.gameOver = gameOver;
        this.suggestedMove = suggestedMove;
        this.username = username;
    }

    public GameStateDTO() {}
}
