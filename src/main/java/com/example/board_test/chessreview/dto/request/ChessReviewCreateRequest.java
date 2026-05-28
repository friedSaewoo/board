package com.example.board_test.chessreview.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ChessReviewCreateRequest(
        @NotBlank String analysisId,
        @NotBlank String aiResponse,
        List<FeedbackMatchRequest> matches
) {
    public ChessReviewCreateRequest {
        matches = matches == null ? List.of() : List.copyOf(matches);
    }
}
