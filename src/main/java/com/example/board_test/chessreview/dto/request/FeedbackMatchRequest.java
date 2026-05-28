package com.example.board_test.chessreview.dto.request;

import com.example.board_test.chess.model.PlayerColor;
import com.example.board_test.chessreview.model.FeedbackMatchConfidence;
import com.example.board_test.chessreview.model.FeedbackMatchSource;

public record FeedbackMatchRequest(
        Integer segmentIndex,
        String text,
        Integer matchedPly,
        Integer matchedMoveNumber,
        PlayerColor matchedSide,
        String matchedSan,
        String matchedUci,
        FeedbackMatchConfidence confidence,
        FeedbackMatchSource source
) {
}
