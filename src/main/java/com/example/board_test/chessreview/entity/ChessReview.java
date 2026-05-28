package com.example.board_test.chessreview.entity;

import com.example.board_test.chess.model.PlayerColor;
import com.example.board_test.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "chess_reviews", indexes = {
        @Index(name = "idx_chess_review_owner", columnList = "ownerMemberId"),
        @Index(name = "idx_chess_review_source_analysis", columnList = "sourceAnalysisId")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChessReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerMemberId;

    @Column(length = 64)
    private String sourceAnalysisId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 120)
    private String whiteName;

    @Column(length = 120)
    private String blackName;

    @Column(length = 24)
    private String result;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PlayerColor playerColor;

    @Column(nullable = false)
    private int moveCount;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String originalPgn;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String metadataJson;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String summaryJson;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String moveAnalysesJson;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String aiPrompt;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String aiResponse;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String feedbackMatchesJson;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String fenSnapshotsJson;

    public static ChessReview fromDraft(
            ChessAnalysisDraft draft,
            String title,
            String whiteName,
            String blackName,
            String result,
            String aiResponse,
            String feedbackMatchesJson,
            String fenSnapshotsJson
    ) {
        return new ChessReview(
                null,
                draft.getOwnerMemberId(),
                draft.getAnalysisId(),
                title,
                whiteName,
                blackName,
                result,
                draft.getPlayerColor(),
                draft.getMoveCount(),
                draft.getOriginalPgn(),
                draft.getMetadataJson(),
                draft.getSummaryJson(),
                draft.getMoveAnalysesJson(),
                draft.getAiPrompt(),
                aiResponse,
                feedbackMatchesJson,
                fenSnapshotsJson
        );
    }

    public void updateFeedbackMatches(String feedbackMatchesJson) {
        this.feedbackMatchesJson = feedbackMatchesJson;
    }
}
