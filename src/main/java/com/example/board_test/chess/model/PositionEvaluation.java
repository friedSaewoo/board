package com.example.board_test.chess.model;

import java.util.List;

public record PositionEvaluation(EngineScore score, String bestMove, List<String> principalVariation) {
    public PositionEvaluation {
        if (score == null) {
            score = EngineScore.cp(0);
        }
        principalVariation = principalVariation == null ? List.of() : List.copyOf(principalVariation);
    }
}
