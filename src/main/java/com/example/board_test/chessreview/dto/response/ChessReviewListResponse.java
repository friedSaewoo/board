package com.example.board_test.chessreview.dto.response;

import com.example.board_test.chess.model.PlayerColor;
import com.example.board_test.chessreview.entity.ChessReview;

import java.time.LocalDateTime;

public record ChessReviewListResponse(
        Long reviewId,
        String title,
        String whiteName,
        String blackName,
        String result,
        PlayerColor playerColor,
        int moveCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ChessReviewListResponse from(ChessReview review) {
        return new ChessReviewListResponse(
                review.getId(),
                review.getTitle(),
                review.getWhiteName(),
                review.getBlackName(),
                review.getResult(),
                review.getPlayerColor(),
                review.getMoveCount(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
