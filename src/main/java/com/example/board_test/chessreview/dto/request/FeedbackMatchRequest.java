package com.example.board_test.chessreview.dto.request;

import com.example.board_test.chessreview.model.FeedbackMatchConfidence;
import com.example.board_test.chessreview.model.FeedbackMatchSource;

public record FeedbackMatchRequest(
        int segmentIndex,
        String text,
        Integer matchedPly,
        FeedbackMatchConfidence confidence,
        FeedbackMatchSource source
) {
}
