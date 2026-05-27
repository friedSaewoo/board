package com.example.board_test.chess.dto.response;

import com.example.board_test.chess.model.MoveClassification;
import com.example.board_test.chess.model.PlayerColor;

import java.util.List;

public record MoveAnalysisResponse(
        int ply,
        int moveNumber,
        PlayerColor side,
        String san,
        String uci,
        int scoreBeforeCp,
        int scoreAfterCp,
        int centipawnLoss,
        MoveClassification classification,
        String bestMove,
        List<String> principalVariation
) {
    public MoveAnalysisResponse {
        principalVariation = principalVariation == null ? List.of() : List.copyOf(principalVariation);
    }
}
