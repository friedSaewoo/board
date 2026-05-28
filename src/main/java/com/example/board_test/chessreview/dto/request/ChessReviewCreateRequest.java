package com.example.board_test.chessreview.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ChessReviewCreateRequest(
        @NotBlank String analysisId,
        @NotBlank String aiResponse,
        @Valid List<FeedbackMatchRequest> matches
) {
}
