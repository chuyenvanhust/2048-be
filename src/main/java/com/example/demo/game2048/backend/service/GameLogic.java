// Đường dẫn: src/main/java/com/example/demo/2048/backend/service/GameLogic.java
package com.example.demo.game2048.backend.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GameLogic {
    public static final int BOARD_SIZE = 4;

    // Records để trả về nhiều giá trị từ một phương thức, giống như tuple trong Python
    public record MoveResult(int[] line, int score, boolean moved) {}
    public record BoardMoveResult(int[][] board, int score, boolean moved) {}

    // Dịch lại hàm compress_line từ Python
    public static MoveResult compressLine(int[] line) {
        List<Integer> nonZero = Arrays.stream(line).filter(i -> i != 0).boxed().collect(Collectors.toList());
        List<Integer> outList = new ArrayList<>();
        int score = 0;

        int i = 0;
        while (i < nonZero.size()) {
            if (i + 1 < nonZero.size() && nonZero.get(i).equals(nonZero.get(i + 1))) {
                int merged = nonZero.get(i) * 2;
                outList.add(merged);
                score += merged;
                i += 2;
            } else {
                outList.add(nonZero.get(i));
                i += 1;
            }
        }

        int[] resultLine = new int[BOARD_SIZE];
        for (int j = 0; j < outList.size(); j++) {
            resultLine[j] = outList.get(j);
        }

        boolean moved = !Arrays.equals(line, resultLine);
        return new MoveResult(resultLine, score, moved);
    }

    public static int[][] transpose(int[][] board) {
        int[][] newBoard = new int[BOARD_SIZE][BOARD_SIZE];
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                newBoard[c][r] = board[r][c];
            }
        }
        return newBoard;
    }

    public static int[][] reverseRows(int[][] board) {
        int[][] newBoard = new int[BOARD_SIZE][BOARD_SIZE];
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                newBoard[r][c] = board[r][BOARD_SIZE - 1 - c];
            }
        }
        return newBoard;
    }

    public static BoardMoveResult moveLeft(int[][] board) {
        int[][] newBoard = new int[BOARD_SIZE][];
        int totalScore = 0;
        boolean anyMoved = false;
        for (int r = 0; r < BOARD_SIZE; r++) {
            MoveResult res = compressLine(board[r]);
            newBoard[r] = res.line();
            totalScore += res.score();
            anyMoved |= res.moved();
        }
        return new BoardMoveResult(newBoard, totalScore, anyMoved);
    }

    public static BoardMoveResult moveRight(int[][] board) {
        int[][] reversed = reverseRows(board);
        BoardMoveResult res = moveLeft(reversed);
        return new BoardMoveResult(reverseRows(res.board()), res.score(), res.moved());
    }

    public static BoardMoveResult moveUp(int[][] board) {
        int[][] tBoard = transpose(board);
        BoardMoveResult res = moveLeft(tBoard);
        return new BoardMoveResult(transpose(res.board()), res.score(), res.moved());
    }

    public static BoardMoveResult moveDown(int[][] board) {
        int[][] tBoard = transpose(board);
        BoardMoveResult res = moveRight(tBoard);
        return new BoardMoveResult(transpose(res.board()), res.score(), res.moved());
    }

    public static List<int[]> getEmptyCells(int[][] board) {
        List<int[]> empty = new ArrayList<>();
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (board[r][c] == 0) {
                    empty.add(new int[]{r, c});
                }
            }
        }
        return empty;
    }

    public static boolean isGameOver(int[][] board) {
        if (!getEmptyCells(board).isEmpty()) return false;
        if (moveLeft(board).moved()) return false;
        if (moveRight(board).moved()) return false;
        if (moveUp(board).moved()) return false;
        return moveDown(board).moved() == false;
    }
}