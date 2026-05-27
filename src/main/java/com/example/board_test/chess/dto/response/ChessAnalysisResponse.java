package com.example.board_test.chess.dto.response;

import com.example.board_test.chess.model.PlayerColor;

import java.util.List;

public record ChessAnalysisResponse(
        GameMetadataResponse metadata,
        PlayerColor playerColor,
        int moveCount,
        AnalysisSummaryResponse summary,
        List<MoveAnalysisResponse> moves,
        String aiPrompt
) {
    public ChessAnalysisResponse {
        moves = moves == null ? List.of() : List.copyOf(moves);
    }
}
