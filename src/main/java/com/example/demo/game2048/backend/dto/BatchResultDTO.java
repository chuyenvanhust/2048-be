package com.example.demo.game2048.backend.dto;

import java.util.Map;

public record BatchResultDTO(
        Map<Integer, Integer> stats, // MaxTile -> Số lần đạt được
        int totalGames,
        long totalTimeMs
) {}