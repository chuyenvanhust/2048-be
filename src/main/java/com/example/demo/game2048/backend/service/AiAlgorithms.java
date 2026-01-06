package com.example.demo.game2048.backend.service;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class AiAlgorithms {

    private int searchDepth = 6;

    public void setDepth(int depth) {
        this.searchDepth = Math.max(1, Math.min(depth, 6));
    }

    public int getDepth() {
        return this.searchDepth;
    }

    /* ================= ALGORITHM SELECTOR ================= */

    public String bestMove(int[][] board, String algorithm) {
        return switch (algorithm.toUpperCase()) {
            case "EXPECTIMAX" -> expectimaxMove(board);
            case "GREEDY" -> greedyMove(board);
            case "BFS" -> bfsMove(board);
            case "DLS" -> dlsMove(board);
            case "IDS" -> idsMove(board);
            case "MINIMAX" -> minimaxMove(board);
            default -> expectimaxMove(board);
        };
    }

    /* ================= EXPECTIMAX ================= */

    private String expectimaxMove(int[][] board) {
        String[] moves = {"UP", "DOWN", "LEFT", "RIGHT"};
        String bestMove = "UP";
        double bestScore = Double.NEGATIVE_INFINITY;

        for (String move : moves) {
            GameLogic.BoardMoveResult result = applyMove(board, move);
            if (!result.moved()) continue;

            double score = expectimax(result.board(), searchDepth - 1, false);
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private double expectimax(int[][] board, int depth, boolean playerTurn) {
        if (depth == 0 || GameLogic.isGameOver(board)) {
            return evaluateBoard(board);
        }

        if (playerTurn) {
            double best = Double.NEGATIVE_INFINITY;
            for (String m : new String[]{"UP","DOWN","LEFT","RIGHT"}) {
                GameLogic.BoardMoveResult r = applyMove(board, m);
                if (r.moved())
                    best = Math.max(best, expectimax(r.board(), depth - 1, false));
            }
            return best == Double.NEGATIVE_INFINITY ? evaluateBoard(board) : best;
        } else {
            List<int[]> empty = GameLogic.getEmptyCells(board);
            if (empty.isEmpty()) return evaluateBoard(board);

            double sum = 0;
            for (int[] c : empty) {
                int[][] b2 = copy(board);
                b2[c[0]][c[1]] = 2;
                sum += 0.9 * expectimax(b2, depth - 1, true);

                int[][] b4 = copy(board);
                b4[c[0]][c[1]] = 4;
                sum += 0.1 * expectimax(b4, depth - 1, true);
            }
            return sum / empty.size();
        }
    }

    /* ================= GREEDY ================= */

    private String greedyMove(int[][] board) {
        String[] moves = {"UP", "DOWN", "LEFT", "RIGHT"};
        String bestMove = "UP";
        double bestScore = Double.NEGATIVE_INFINITY;

        for (String move : moves) {
            GameLogic.BoardMoveResult result = applyMove(board, move);
            if (!result.moved()) continue;

            double score = evaluateBoard(result.board()) + result.score();
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    /* ================= BFS ================= */

    private String bfsMove(int[][] board) {
        String[] moves = {"UP", "DOWN", "LEFT", "RIGHT"};
        Map<String, Double> moveScores = new HashMap<>();

        Queue<SearchNode> queue = new LinkedList<>();

        for (String move : moves) {
            GameLogic.BoardMoveResult result = applyMove(board, move);
            if (result.moved()) {
                queue.offer(new SearchNode(result.board(), move, 0));
            }
        }

        while (!queue.isEmpty()) {
            SearchNode node = queue.poll();

            if (node.depth >= searchDepth) {
                double score = evaluateBoard(node.board);
                moveScores.merge(node.firstMove, score, Math::max);
                continue;
            }

            for (String move : moves) {
                GameLogic.BoardMoveResult result = applyMove(node.board, move);
                if (result.moved()) {
                    queue.offer(new SearchNode(result.board(), node.firstMove, node.depth + 1));
                }
            }
        }

        return moveScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("UP");
    }

    /* ================= DLS ================= */

    private String dlsMove(int[][] board) {
        String[] moves = {"UP", "DOWN", "LEFT", "RIGHT"};
        String bestMove = "UP";
        double bestScore = Double.NEGATIVE_INFINITY;

        for (String move : moves) {
            GameLogic.BoardMoveResult result = applyMove(board, move);
            if (!result.moved()) continue;

            double score = dls(result.board(), searchDepth - 1);
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private double dls(int[][] board, int depth) {
        if (depth == 0 || GameLogic.isGameOver(board)) {
            return evaluateBoard(board);
        }

        double maxScore = Double.NEGATIVE_INFINITY;
        for (String move : new String[]{"UP", "DOWN", "LEFT", "RIGHT"}) {
            GameLogic.BoardMoveResult result = applyMove(board, move);
            if (result.moved()) {
                maxScore = Math.max(maxScore, dls(result.board(), depth - 1));
            }
        }
        return maxScore == Double.NEGATIVE_INFINITY ? evaluateBoard(board) : maxScore;
    }

    /* ================= IDS (Iterative Deepening Search) ================= */

    private String idsMove(int[][] board) {
        String bestMove = "UP";

        for (int depth = 1; depth <= searchDepth; depth++) {
            String move = dlsMove(board, depth);
            if (move != null) {
                bestMove = move;
            }
        }
        return bestMove;
    }

    private String dlsMove(int[][] board, int maxDepth) {
        String[] moves = {"UP", "DOWN", "LEFT", "RIGHT"};
        String bestMove = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (String move : moves) {
            GameLogic.BoardMoveResult result = applyMove(board, move);
            if (!result.moved()) continue;

            double score = dls(result.board(), maxDepth - 1);
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    /* ================= MINIMAX ================= */

    private String minimaxMove(int[][] board) {
        String[] moves = {"UP", "DOWN", "LEFT", "RIGHT"};
        String bestMove = "UP";
        double bestScore = Double.NEGATIVE_INFINITY;

        for (String move : moves) {
            GameLogic.BoardMoveResult result = applyMove(board, move);
            if (!result.moved()) continue;

            double score = minimax(result.board(), searchDepth - 1, false);
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private double minimax(int[][] board, int depth, boolean maximizing) {
        if (depth == 0 || GameLogic.isGameOver(board)) {
            return evaluateBoard(board);
        }

        if (maximizing) {
            double maxEval = Double.NEGATIVE_INFINITY;
            for (String move : new String[]{"UP", "DOWN", "LEFT", "RIGHT"}) {
                GameLogic.BoardMoveResult result = applyMove(board, move);
                if (result.moved()) {
                    double eval = minimax(result.board(), depth - 1, false);
                    maxEval = Math.max(maxEval, eval);
                }
            }
            return maxEval == Double.NEGATIVE_INFINITY ? evaluateBoard(board) : maxEval;
        } else {
            List<int[]> empty = GameLogic.getEmptyCells(board);
            if (empty.isEmpty()) return evaluateBoard(board);

            double minEval = Double.POSITIVE_INFINITY;
            for (int[] c : empty) {
                int[][] b2 = copy(board);
                b2[c[0]][c[1]] = 2;
                minEval = Math.min(minEval, minimax(b2, depth - 1, true));
            }
            return minEval;
        }
    }

    /* ================= HEURISTIC EVALUATION ================= */

    private double evaluateBoard(int[][] b) {
        double snakeScore = snakePattern(b);
        double monotonicityScore = improvedMonotonicity(b);
        double smoothScore = improvedSmoothness(b);
        double emptyScore = countEmpty(b);
        double mergeScore = mergePotential(b);

        return 10000.0 * snakeScore +
                1000.0 * monotonicityScore +
                100.0 * smoothScore +
                270.0 * emptyScore +
                300.0 * mergeScore;
    }

    private double snakePattern(int[][] b) {
        int[][] weights = {
                {15, 14, 13, 12},
                {8,  9,  10, 11},
                {7,  6,  5,  4},
                {0,  1,  2,  3}
        };

        double score = 0;
        int maxTile = 0;

        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (b[r][c] > 0) {
                    double value = Math.pow(2, log2(b[r][c]) * 1.5);
                    score += value * Math.pow(2, weights[r][c]);
                    maxTile = Math.max(maxTile, b[r][c]);
                }
            }
        }

        if (b[0][0] == maxTile) {
            score *= 2.0;
        }

        return score;
    }

    private double improvedMonotonicity(int[][] b) {
        double[] totals = new double[4];

        for (int r = 0; r < 4; r++) {
            int current = 0;
            int next = current + 1;
            while (next < 4) {
                while (next < 4 && b[r][next] == 0) next++;
                if (next >= 4) break;

                double currentValue = b[r][current] > 0 ? log2(b[r][current]) : 0;
                double nextValue = log2(b[r][next]);

                if (currentValue > nextValue) {
                    totals[0] += nextValue - currentValue;
                } else if (nextValue > currentValue) {
                    totals[1] += currentValue - nextValue;
                }

                current = next;
                next++;
            }
        }

        for (int c = 0; c < 4; c++) {
            int current = 0;
            int next = current + 1;
            while (next < 4) {
                while (next < 4 && b[next][c] == 0) next++;
                if (next >= 4) break;

                double currentValue = b[current][c] > 0 ? log2(b[current][c]) : 0;
                double nextValue = log2(b[next][c]);

                if (currentValue > nextValue) {
                    totals[2] += nextValue - currentValue;
                } else if (nextValue > currentValue) {
                    totals[3] += currentValue - nextValue;
                }

                current = next;
                next++;
            }
        }

        return Math.max(totals[0], totals[1]) + Math.max(totals[2], totals[3]);
    }

    private double improvedSmoothness(int[][] b) {
        double smooth = 0;

        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (b[r][c] == 0) continue;

                double value = log2(b[r][c]);

                if (c + 1 < 4 && b[r][c + 1] != 0) {
                    double targetValue = log2(b[r][c + 1]);
                    smooth -= Math.abs(value - targetValue);
                }

                if (r + 1 < 4 && b[r + 1][c] != 0) {
                    double targetValue = log2(b[r + 1][c]);
                    smooth -= Math.abs(value - targetValue);
                }
            }
        }

        return smooth;
    }

    private double log2(int v) {
        return v <= 0 ? 0 : Math.log(v) / Math.log(2);
    }

    private int countEmpty(int[][] b) {
        int count = 0;
        for (int[] row : b) {
            for (int v : row) {
                if (v == 0) count++;
            }
        }
        return count;
    }

    private double mergePotential(int[][] b) {
        double score = 0;

        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                if (b[r][c] != 0 && b[r][c] == b[r][c + 1]) {
                    score += log2(b[r][c]);
                }
            }
        }

        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 3; r++) {
                if (b[r][c] != 0 && b[r][c] == b[r + 1][c]) {
                    score += log2(b[r][c]);
                }
            }
        }

        return score;
    }

    /* ================= UTILITY METHODS ================= */

    private GameLogic.BoardMoveResult applyMove(int[][] board, String dir) {
        return switch (dir) {
            case "UP" -> GameLogic.moveUp(board);
            case "DOWN" -> GameLogic.moveDown(board);
            case "LEFT" -> GameLogic.moveLeft(board);
            case "RIGHT" -> GameLogic.moveRight(board);
            default -> new GameLogic.BoardMoveResult(board, 0, false);
        };
    }

    private int[][] copy(int[][] b) {
        int[][] n = new int[4][4];
        for (int i = 0; i < 4; i++)
            System.arraycopy(b[i], 0, n[i], 0, 4);
        return n;
    }

    /* ================= HELPER CLASSES ================= */

    private static class SearchNode {
        int[][] board;
        String firstMove;
        int depth;

        SearchNode(int[][] board, String firstMove, int depth) {
            this.board = board;
            this.firstMove = firstMove;
            this.depth = depth;
        }
    }
}