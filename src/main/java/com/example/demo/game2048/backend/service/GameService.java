package com.example.demo.game2048.backend.service;

import com.example.demo.game2048.backend.dto.GameStateDTO;
import com.example.demo.game2048.backend.dto.BatchResultDTO;
import com.example.demo.game2048.backend.entity.GameRecord;
import com.example.demo.game2048.backend.repository.GameRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    // Key: sessionId:boardId -> BoardState
    private final Map<String, BoardState> boards = new ConcurrentHashMap<>();
    private final Map<String, Integer> boardDepths = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private int defaultAiDepth = 3;

    @Autowired
    private AiAlgorithms aiAlgorithms;

    @Autowired
    private GameRecordRepository repository;

    @Autowired
    private SessionService sessionService;

    private static class BoardState {
        int[][] board;
        int score;
        boolean gameOver;
        String currentUsername;
        int hintsUsed;
        boolean isAiStarted;
        String lastSuggestedMove;
        String lastSuggester;
        String lastUsedAlgorithm;

        BoardState(String username) {
            this.board = new int[GameLogic.BOARD_SIZE][GameLogic.BOARD_SIZE];
            this.score = 0;
            this.gameOver = false;
            this.currentUsername = username != null && !username.isEmpty() ? username : "Guest";
            this.hintsUsed = 0;
            this.isAiStarted = false;
            this.lastSuggestedMove = null;
            this.lastSuggester = null;
            this.lastUsedAlgorithm = null;
        }
    }

    /**
     * Generate board key from sessionId and boardId
     */
    private String getBoardKey(String sessionId, int boardId) {
        return sessionId + ":" + boardId;
    }

    /**
     * Get or create board state for a specific session and board
     */
    private BoardState getBoard(String sessionId, int boardId) {
        String boardKey = getBoardKey(sessionId, boardId);

        return boards.computeIfAbsent(boardKey, key -> {
            BoardState state = new BoardState("Guest");
            addRandomTile(state);
            addRandomTile(state);
            sessionService.registerBoard(sessionId, boardKey);
            return state;
        });
    }

    public void startNewGame(String sessionId, String username, int boardId) {
        if (!sessionService.isValidSession(sessionId)) {
            throw new IllegalStateException("Invalid or expired session");
        }

        String boardKey = getBoardKey(sessionId, boardId);
        BoardState state = new BoardState(username);
        addRandomTile(state);
        addRandomTile(state);
        boards.put(boardKey, state);

        sessionService.registerBoard(sessionId, boardKey);
        sessionService.touchSession(sessionId);
    }

    public void move(String sessionId, String direction, int boardId) {
        if (!sessionService.isValidSession(sessionId)) {
            throw new IllegalStateException("Invalid or expired session");
        }

        BoardState state = getBoard(sessionId, boardId);
        sessionService.touchSession(sessionId);

        if (state.gameOver) {
            return;
        }

        GameLogic.BoardMoveResult result;
        switch (direction) {
            case "UP" -> result = GameLogic.moveUp(state.board);
            case "DOWN" -> result = GameLogic.moveDown(state.board);
            case "LEFT" -> result = GameLogic.moveLeft(state.board);
            case "RIGHT" -> result = GameLogic.moveRight(state.board);
            default -> {
                System.out.println("Invalid direction: " + direction);
                return;
            }
        }

        if (result.moved()) {
            state.board = result.board();
            state.score += result.score();
            addRandomTile(state);

            if (GameLogic.isGameOver(state.board)) {
                state.gameOver = true;
                saveGame(state);
                System.out.println("Session " + sessionId + " Board " + boardId + " - Game Over! Final Score: " + state.score);
            }
        }

        state.lastSuggestedMove = null;
        state.lastSuggester = null;
    }

    private void addRandomTile(BoardState state) {
        List<int[]> emptyCells = GameLogic.getEmptyCells(state.board);
        if (!emptyCells.isEmpty()) {
            int[] cell = emptyCells.get(random.nextInt(emptyCells.size()));
            state.board[cell[0]][cell[1]] = random.nextDouble() < 0.9 ? 2 : 4;
        }
    }

    public GameStateDTO getGameState(String sessionId, int boardId) {
        if (!sessionService.isValidSession(sessionId)) {
            throw new IllegalStateException("Invalid or expired session");
        }

        BoardState state = getBoard(sessionId, boardId);
        sessionService.touchSession(sessionId);

        return new GameStateDTO(
                state.board,
                state.score,
                state.gameOver,
                state.lastSuggestedMove,
                state.lastSuggester
        );
    }

    public GameStateDTO getHint(String sessionId, int boardId, String algorithm) {
        if (!sessionService.isValidSession(sessionId)) {
            throw new IllegalStateException("Invalid or expired session");
        }

        BoardState state = getBoard(sessionId, boardId);
        sessionService.touchSession(sessionId);

        if (state.gameOver) {
            state.lastSuggestedMove = null;
            state.lastSuggester = null;
        } else {
            state.hintsUsed++;
            String boardKey = getBoardKey(sessionId, boardId);
            int depth = boardDepths.getOrDefault(boardKey, defaultAiDepth);
            aiAlgorithms.setDepth(depth);
            String bestMove = aiAlgorithms.bestMove(state.board, algorithm);
            if (bestMove != null) {
                state.lastSuggestedMove = bestMove;
                state.lastSuggester = algorithm.toUpperCase() + " AI";
            } else {
                state.lastSuggestedMove = null;
                state.lastSuggester = null;
            }
        }
        return getGameState(sessionId, boardId);
    }

    public String getAiMove(String sessionId, int boardId, String algorithm) {
        if (!sessionService.isValidSession(sessionId)) {
            throw new IllegalStateException("Invalid or expired session");
        }

        BoardState state = getBoard(sessionId, boardId);
        sessionService.touchSession(sessionId);

        if (state.gameOver) {
            System.out.println("Session " + sessionId + " Board " + boardId + " - Game is already over");
            return null;
        }

        state.lastUsedAlgorithm = algorithm;

        String boardKey = getBoardKey(sessionId, boardId);
        int depth = boardDepths.getOrDefault(boardKey, defaultAiDepth);
        aiAlgorithms.setDepth(depth);
        String bestMove = aiAlgorithms.bestMove(state.board, algorithm);

        if (bestMove == null) {
            System.out.println("Session " + sessionId + " Board " + boardId + " - No valid moves available");
            return null;
        }

        GameLogic.BoardMoveResult testResult = switch (bestMove) {
            case "UP" -> GameLogic.moveUp(state.board);
            case "DOWN" -> GameLogic.moveDown(state.board);
            case "LEFT" -> GameLogic.moveLeft(state.board);
            case "RIGHT" -> GameLogic.moveRight(state.board);
            default -> new GameLogic.BoardMoveResult(state.board, 0, false);
        };

        if (!testResult.moved()) {
            System.out.println("Session " + sessionId + " Board " + boardId + " - AI suggested invalid move: " + bestMove);
            return null;
        }

        System.out.println("Session " + sessionId + " Board " + boardId + " - " + algorithm + " AI suggests: " + bestMove);
        return bestMove;
    }

    public void markAiStarted(String sessionId, int boardId) {
        if (!sessionService.isValidSession(sessionId)) {
            throw new IllegalStateException("Invalid or expired session");
        }

        BoardState state = getBoard(sessionId, boardId);
        state.isAiStarted = true;
        sessionService.touchSession(sessionId);
    }

    public void setAiDepth(int depth) {
        this.defaultAiDepth = Math.max(1, Math.min(depth, 10));
        aiAlgorithms.setDepth(this.defaultAiDepth);
    }

    public void setAiDepthForBoard(String sessionId, int boardId, int depth) {
        if (!sessionService.isValidSession(sessionId)) {
            throw new IllegalStateException("Invalid or expired session");
        }

        String boardKey = getBoardKey(sessionId, boardId);
        int validDepth = Math.max(1, Math.min(depth, 10));
        boardDepths.put(boardKey, validDepth);
        sessionService.touchSession(sessionId);
    }

    private void saveGame(BoardState state) {
        String gameMode;
        if (state.isAiStarted && state.lastUsedAlgorithm != null) {
            gameMode = "SINGLE_" + state.lastUsedAlgorithm.toUpperCase();
        } else if (state.isAiStarted) {
            gameMode = "SINGLE_AI";
        } else {
            gameMode = "SINGLE_MANUAL";
        }

        GameRecord record = new GameRecord();
        record.setUsername(state.currentUsername);
        record.setGameMode(gameMode);
        record.setScore(state.score);
        record.setMaxTile(calculateMaxTile(state.board));
        record.setHintsUsed(state.hintsUsed);
        repository.save(record);
    }

    public BatchResultDTO runBatchGames(String sessionId, int count, String username, String algorithm, int boardId) {
        if (!sessionService.isValidSession(sessionId)) {
            throw new IllegalStateException("Invalid or expired session");
        }

        sessionService.touchSession(sessionId);

        long startTime = System.currentTimeMillis();
        Map<Integer, Integer> stats = new HashMap<>();

        String boardKey = getBoardKey(sessionId, boardId);
        int depth = boardDepths.getOrDefault(boardKey, defaultAiDepth);
        aiAlgorithms.setDepth(depth);

        for (int i = 0; i < count; i++) {
            int[][] tempBoard = new int[GameLogic.BOARD_SIZE][GameLogic.BOARD_SIZE];
            int tempScore = 0;
            addRandomTileToBoard(tempBoard);
            addRandomTileToBoard(tempBoard);

            boolean isDone = false;
            int moveCount = 0;
            int maxMoves = 10000;

            while (!isDone && moveCount < maxMoves) {
                String bestMove = aiAlgorithms.bestMove(tempBoard, algorithm);
                if (bestMove == null) break;

                GameLogic.BoardMoveResult res = switch (bestMove) {
                    case "UP" -> GameLogic.moveUp(tempBoard);
                    case "DOWN" -> GameLogic.moveDown(tempBoard);
                    case "LEFT" -> GameLogic.moveLeft(tempBoard);
                    case "RIGHT" -> GameLogic.moveRight(tempBoard);
                    default -> new GameLogic.BoardMoveResult(tempBoard, 0, false);
                };

                if (res.moved()) {
                    tempBoard = res.board();
                    tempScore += res.score();
                    addRandomTileToBoard(tempBoard);
                    if (GameLogic.isGameOver(tempBoard)) {
                        isDone = true;
                    }
                } else {
                    isDone = true;
                }
                moveCount++;
            }

            int maxTile = calculateMaxTile(tempBoard);

            GameRecord record = new GameRecord();
            record.setUsername(username);
            record.setGameMode("BATCH_" + algorithm.toUpperCase());
            record.setScore(tempScore);
            record.setMaxTile(maxTile);
            record.setHintsUsed(0);
            repository.save(record);

            stats.put(maxTile, stats.getOrDefault(maxTile, 0) + 1);
        }

        long endTime = System.currentTimeMillis();
        return new BatchResultDTO(stats, count, (endTime - startTime));
    }

    private void addRandomTileToBoard(int[][] targetBoard) {
        List<int[]> emptyCells = GameLogic.getEmptyCells(targetBoard);
        if (!emptyCells.isEmpty()) {
            int[] cell = emptyCells.get(random.nextInt(emptyCells.size()));
            targetBoard[cell[0]][cell[1]] = random.nextDouble() < 0.9 ? 2 : 4;
        }
    }

    private int calculateMaxTile(int[][] b) {
        int max = 0;
        for (int[] row : b) {
            for (int cell : row) {
                max = Math.max(max, cell);
            }
        }
        return max;
    }

    public void clearBoard(String sessionId, int boardId) {
        if (!sessionService.isValidSession(sessionId)) {
            throw new IllegalStateException("Invalid or expired session");
        }

        String boardKey = getBoardKey(sessionId, boardId);
        boards.remove(boardKey);
        boardDepths.remove(boardKey);
        sessionService.touchSession(sessionId);
    }

    public void clearSessionBoards(String sessionId) {
        if (!sessionService.isValidSession(sessionId)) {
            return;
        }

        Set<String> sessionBoards = sessionService.getSessionBoards(sessionId);
        for (String boardKey : sessionBoards) {
            boards.remove(boardKey);
            boardDepths.remove(boardKey);
        }
        sessionService.cleanupSession(sessionId);
    }

    public void updateBoardUsername(String sessionId, int boardId, String newUsername) {
        if (!sessionService.isValidSession(sessionId)) {
            throw new IllegalStateException("Invalid or expired session");
        }

        BoardState state = getBoard(sessionId, boardId);
        state.currentUsername = newUsername != null && !newUsername.isEmpty() ? newUsername : "Guest";
        sessionService.touchSession(sessionId);
    }
}