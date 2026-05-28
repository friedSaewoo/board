package com.example.board_test.chessreview.dto.response;

import com.example.board_test.chess.dto.response.AnalysisSummaryResponse;
import com.example.board_test.chess.dto.response.GameMetadataResponse;
import com.example.board_test.chess.dto.response.MoveAnalysisResponse;
import com.example.board_test.chess.model.PlayerColor;
import com.example.board_test.chessreview.entity.ChessReview;

import java.time.LocalDateTime;
import java.util.List;

public record ChessReviewResponse(
        Long id,
        String sourceAnalysisId,
        String title,
        String whiteName,
        String blackName,
        String result,
        PlayerColor playerColor,
        int moveCount,
        String originalPgn,
        GameMetadataResponse metadata,
        AnalysisSummaryResponse summary,
        List<MoveAnalysisResponse> moves,
        String aiPrompt,
        String aiResponse,
        List<FeedbackMatchResponse> feedbackMatches,
        String fenSnapshotsJson,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ChessReviewResponse from(
            ChessReview review,
            GameMetadataResponse metadata,
            AnalysisSummaryResponse summary,
            List<MoveAnalysisResponse> moves,
            List<FeedbackMatchResponse> feedbackMatches
    ) {
        return new ChessReviewResponse(
                review.getId(),
                review.getSourceAnalysisId(),
                review.getTitle(),
                review.getWhiteName(),
                review.getBlackName(),
                review.getResult(),
                review.getPlayerColor(),
                review.getMoveCount(),
                review.getOriginalPgn(),
                metadata,
                summary,
                moves,
                review.getAiPrompt(),
                review.getAiResponse(),
                feedbackMatches,
                review.getFenSnapshotsJson(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
