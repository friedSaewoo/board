package com.example.board_test.chessreview.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ChessReviewMatchUpdateRequest(
        @NotNull @Valid List<FeedbackMatchRequest> matches
) {
    public ChessReviewMatchUpdateRequest {
        matches = matches == null ? List.of() : List.copyOf(matches);
    }
}
