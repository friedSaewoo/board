package com.example.board_test.chess.dto.request;

import com.example.board_test.chess.model.PlayerColor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChessAnalysisRequest(
        @NotBlank String pgn,
        @NotNull PlayerColor playerColor
) {
}
