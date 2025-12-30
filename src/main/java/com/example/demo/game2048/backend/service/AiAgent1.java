package com.example.demo.game2048.backend.service;

import org.springframework.stereotype.Component;
import java.util.Random;

@Component
public class AiAgent1 {
    private final MoveTable moveTable;
    private final Heuristic heuristic;
    private final TranspositionTable hash;
    private final int MIN_DEPTH = 3;
    private final int MAX_DEPTH = 12;

    private int stateEvaled = 0;
    private float minProb;

    public AiAgent1() {
        this.moveTable = new MoveTable();
        this.heuristic = new Heuristic();
        this.hash = new TranspositionTable();
    }

    public String bestMove(int[][] grid) {
        long board = encode(grid);
        float maxScore = -1e18f;
        int bestDir = -1;

        for (int i = 0; i < 4; i++) {
            long next = moveTable.apply(board, i);
            if (next == board) continue;

            float result = search(board, i);
            if (result > maxScore) {
                maxScore = result;
                bestDir = i;
            }
        }

        if (bestDir == -1) {
            for (int i = 0; i < 4; i++) {
                if (moveTable.apply(board, i) != board) return getDirStr(i);
            }
            return "UP";
        }
        return getDirStr(bestDir);
    }

    private String getDirStr(int dir) {
        return switch (dir) {
            case 0 -> "UP";
            case 1 -> "RIGHT";
            case 2 -> "DOWN";
            case 3 -> "LEFT";
            default -> "UP";
        };
    }

    private float search(long s, int moveDir) {
        long newBoard = moveTable.apply(s, moveDir);
        stateEvaled = 0;
        int currentDepth = MIN_DEPTH;

        minProb = 1.0f / (float) (1 << (2 * currentDepth + 5));
        float result = expectimaxSpawnNode(newBoard, currentDepth, 1.0f);

        int minState = 1 << (3 * currentDepth + 5);
        int lastStates = 0;

        while (currentDepth < MAX_DEPTH && stateEvaled < minState && stateEvaled > lastStates) {
            lastStates = stateEvaled;
            stateEvaled = 0;
            currentDepth++;
            minProb = 1.0f / (float) (1 << (2 * currentDepth + 5));
            minState *= 2;
            result = expectimaxSpawnNode(newBoard, currentDepth, 1.0f);
            if (stateEvaled > 500000) break;
        }
        return result;
    }

    private float expectimaxSpawnNode(long s, int depth, float prob) {
        if (depth <= 0 || prob < minProb) return heuristic.eval(s);

        TTEntry entry = hash.lookup(s);
        if (entry != null && entry.depth >= depth) {
            stateEvaled += Math.pow(entry.moves, (float) depth / entry.depth);
            return entry.score;
        }

        int emptyTiles = countEmpty(s);
        if (emptyTiles == 0) return heuristic.eval(s);

        float expect = 0.0f;
        float prob2 = prob * 0.9f / emptyTiles;
        float prob4 = prob * 0.1f / emptyTiles;

        int startEval = stateEvaled;
        long tmp = s;
        for (int i = 0; i < 16; i++) {
            if ((tmp & 0xF) == 0) {
                expect += expectimaxMoveNode(s | (1L << (i * 4)), depth - 1, prob2) * 0.9f;
                expect += expectimaxMoveNode(s | (2L << (i * 4)), depth - 1, prob4) * 0.1f;
            }
            tmp >>>= 4;
        }

        float res = expect / emptyTiles;
        hash.update(s, depth, res, stateEvaled - startEval);
        return res;
    }

    private float expectimaxMoveNode(long s, int depth, float prob) {
        stateEvaled++;
        float max = 0.0f;
        for (int i = 0; i < 4; i++) {
            long next = moveTable.apply(s, i);
            if (next != s) {
                float res = expectimaxSpawnNode(next, depth, prob);
                if (res > max) max = res;
            }
        }
        return max;
    }

    private int countEmpty(long b) {
        int count = 0;
        for (int i = 0; i < 16; i++) {
            if (((b >>> (i * 4)) & 0xF) == 0) count++;
        }
        return count;
    }

    private long encode(int[][] grid) {
        long board = 0;
        for (int i = 0; i < 16; i++) {
            int r = i / 4;
            int c = i % 4;
            int val = grid[r][c];
            int rank = (val <= 0) ? 0 : Integer.numberOfTrailingZeros(val);
            board |= ((long) rank << ((15 - i) * 4));
        }
        return board;
    }

    // ============= MOVE TABLE =============
    static class MoveTable {
        private final short[] moveTable = new short[65536];
        private final short[] revTable = new short[65536];

        public MoveTable() {
            for (int row = 0; row < 65536; row++) {
                int[] line = {(row & 0xF), (row >>> 4) & 0xF, (row >>> 8) & 0xF, (row >>> 12) & 0xF};
                int farthest = 3;
                boolean merged = false;
                for (int i = 3; i >= 0; i--) {
                    if (line[i] == 0) continue;
                    if (!merged && farthest < 3 && line[i] == line[farthest + 1]) {
                        line[farthest + 1] = (line[farthest + 1] + 1) & 0xF;
                        line[i] = 0;
                        merged = true;
                    } else if (farthest == i) {
                        farthest--;
                    } else {
                        line[farthest--] = line[i];
                        line[i] = 0;
                        merged = false;
                    }
                }
                short result = (short) (line[0] | (line[1] << 4) | (line[2] << 8) | (line[3] << 12));
                moveTable[row] = result;
                int revRow = reverseRow(row);
                revTable[revRow] = (short) reverseRow(result);
            }
        }

        private int reverseRow(int r) {
            return ((r >> 12) & 0xF) | ((r >> 4) & 0xF0) | ((r << 4) & 0xF00) | ((r << 12) & 0xF000);
        }

        public long apply(long s, int dir) {
            return switch (dir) {
                case 0 -> Heuristic.transpose(moveLeft(Heuristic.transpose(s)));
                case 1 -> moveRight(s);
                case 2 -> Heuristic.transpose(moveRight(Heuristic.transpose(s)));
                case 3 -> moveLeft(s);
                default -> s;
            };
        }

        private long moveLeft(long s) {
            return ((long) (moveTable[(int) (s & 0xFFFF)] & 0xFFFF)) |
                    ((long) (moveTable[(int) ((s >>> 16) & 0xFFFF)] & 0xFFFF) << 16) |
                    ((long) (moveTable[(int) ((s >>> 32) & 0xFFFF)] & 0xFFFF) << 32) |
                    ((long) (moveTable[(int) ((s >>> 48) & 0xFFFF)] & 0xFFFF) << 48);
        }

        private long moveRight(long s) {
            return ((long) (revTable[(int) (s & 0xFFFF)] & 0xFFFF)) |
                    ((long) (revTable[(int) ((s >>> 16) & 0xFFFF)] & 0xFFFF) << 16) |
                    ((long) (revTable[(int) ((s >>> 32) & 0xFFFF)] & 0xFFFF) << 32) |
                    ((long) (revTable[(int) ((s >>> 48) & 0xFFFF)] & 0xFFFF) << 48);
        }
    }

    // ============= HEURISTIC =============
    static class Heuristic {
        private final float[] heurTable = new float[65536];
        private static final float SNAKE_FACTOR = 0.0f;
        private static final int[] SNAKE_WEIGHTS = {
                15, 14, 13, 12,
                8,  9, 10, 11,
                7,  6,  5,  4,
                0,  1,  2,  3
        };

        public Heuristic() {
            for (int row = 0; row < 65536; row++) {
                int[] line = {(row & 0xF), (row >>> 4) & 0xF, (row >>> 8) & 0xF, (row >>> 12) & 0xF};
                float sum = 0;
                int empty = 0, merges = 0, prev = 0, counter = 0;

                for (int rank : line) {
                    sum += Math.pow(rank, 3.5);
                    if (rank == 0) empty++;
                    else {
                        if (prev == rank) counter++;
                        else {
                            if (counter > 0) merges += 1 + counter;
                            counter = 0;
                        }
                        prev = rank;
                    }
                }
                if (counter > 0) merges += 1 + counter;

                float monoLeft = 0, monoRight = 0;
                for (int i = 1; i < 4; i++) {
                    if (line[i - 1] > line[i]) monoLeft += Math.pow(line[i - 1], 4) - Math.pow(line[i], 4);
                    else monoRight += Math.pow(line[i], 4) - Math.pow(line[i - 1], 4);
                }

                heurTable[row] = 200000.0f + 270.0f * empty + 700.0f * merges
                        - 47.0f * Math.min(monoLeft, monoRight) - 11.0f * sum;
            }
        }

        public float eval(long s) {
            float base = scoreHeuristic(s) + scoreHeuristic(transpose(s));
            // Tìm snake score cao nhất trong 8 hướng đối xứng
            float snake = maxSnake(s);
            return base + (SNAKE_FACTOR * snake);
        }

        private float scoreHeuristic(long s) {
            return heurTable[(int) (s & 0xFFFF)] + heurTable[(int) ((s >>> 16) & 0xFFFF)] +
                    heurTable[(int) ((s >>> 32) & 0xFFFF)] + heurTable[(int) ((s >>> 48) & 0xFFFF)];
        }

        private float snakeScore(long s) {
            float score = 0;
            for (int i = 0; i < 16; i++) {
                int rank = (int)((s >>> (i * 4)) & 0xF);
                if (rank != 0) {
                    score += rank * SNAKE_WEIGHTS[i];
                }
            }
            return score;
        }

        /**
         * Duyệt qua 8 hướng (4 xoay x 2 lật) để tìm snake score lớn nhất
         */
        private float maxSnake(long s) {
            float best = 0;
            long current = s;
            for (int i = 0; i < 4; i++) {
                // Thử hướng hiện tại
                best = Math.max(best, snakeScore(current));
                // Thử hướng đối xứng qua đường chéo (transpose) của hướng hiện tại
                best = Math.max(best, snakeScore(transpose(current)));
                // Xoay bàn cờ 90 độ cho vòng lặp kế tiếp
                current = rotate90(current);
            }
            return best;
        }

        // --- Các hàm hỗ trợ Bitwise Manipulation ---
        public static long transpose(long x) {
            long t = (x ^ (x >>> 12)) & 0x0000F0F00000F0F0L;
            x ^= t ^ (t << 12);
            t = (x ^ (x >>> 24)) & 0x00000000FF00FF00L;
            x ^= t ^ (t << 24);
            return x;
        }

        public static long reverse(long x) {
            long r = 0;
            for (int i = 0; i < 4; i++) {
                long row = (x >>> (i * 16)) & 0xFFFF;
                row = ((row >> 12) & 0xF) | ((row >> 4) & 0xF0) | ((row << 4) & 0xF00) | ((row << 12) & 0xF000);
                r |= row << (i * 16);
            }
            return r;
        }

        public static long rotate90(long x) {
            return reverse(transpose(x));
        }
    }

    // ============= TRANSPOSITION TABLE =============
    static class TTEntry {
        long board; float score; int depth; int moves;
    }

    static class TranspositionTable {
        private static final int SIZE = 0x400000;
        private final TTEntry[] entries = new TTEntry[SIZE];
        private final int[] zMap = new int[256];

        public TranspositionTable() {
            Random r = new Random();
            for (int i = 0; i < 256; i++) zMap[i] = r.nextInt(0x3FFFFF);
        }

        private int getHash(long x) {
            int h = 0;
            for (int i = 0; i < 16; i++) {
                h ^= zMap[(i << 4) | (int) (x & 0xF)];
                x >>>= 4;
            }
            return h & (SIZE - 1);
        }

        public TTEntry lookup(long b) {
            TTEntry e = entries[getHash(b)];
            return (e != null && e.board == b) ? e : null;
        }

        public void update(long b, int d, float s, int m) {
            int h = getHash(b);
            if (entries[h] == null) entries[h] = new TTEntry();
            entries[h].board = b;
            entries[h].score = s;
            entries[h].depth = d;
            entries[h].moves = m;
        }
    }
}