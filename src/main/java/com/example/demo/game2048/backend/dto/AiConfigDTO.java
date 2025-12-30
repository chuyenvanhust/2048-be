package com.example.demo.game2048.backend.dto;

import lombok.Data;

@Data
public class AiConfigDTO {
    private int depth;

    public int getDepth() {
        return depth;
    }
}
