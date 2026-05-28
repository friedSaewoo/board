package com.example.board_test.chess.dto.response;

import com.example.board_test.chess.model.PlayerColor;

import java.util.List;

public record ChessAnalysisResponse(
        String analysisId,
        GameMetadataResponse metadata,
        PlayerColor playerColor,
        int moveCount,
        AnalysisSummaryResponse summary,
        List<MoveAnalysisResponse> moves,
        String aiPrompt,
        String analysisId
) {
    public ChessAnalysisResponse {
        moves = moves == null ? List.of() : List.copyOf(moves);
    }

    public ChessAnalysisResponse(
            GameMetadataResponse metadata,
            PlayerColor playerColor,
            int moveCount,
            AnalysisSummaryResponse summary,
            List<MoveAnalysisResponse> moves,
            String aiPrompt
    ) {
        this(null, metadata, playerColor, moveCount, summary, moves, aiPrompt);
    }
}
