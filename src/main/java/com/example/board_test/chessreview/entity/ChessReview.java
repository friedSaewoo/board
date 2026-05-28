package com.example.board_test.chessreview.entity;

import com.example.board_test.chess.model.PlayerColor;
import com.example.board_test.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "chess_reviews")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChessReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerMemberId;

    @Column(length = 36)
    private String sourceAnalysisId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 100)
    private String whiteName;

    @Column(length = 100)
    private String blackName;

    @Column(length = 20)
    private String result;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
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
            String whiteName,
            String blackName,
            String result,
            String aiResponse,
            String feedbackMatchesJson
    ) {
        return ChessReview.builder()
                .ownerMemberId(draft.getOwnerMemberId())
                .sourceAnalysisId(draft.getAnalysisId())
                .title(draft.getTitle())
                .whiteName(whiteName)
                .blackName(blackName)
                .result(result)
                .playerColor(draft.getPlayerColor())
                .moveCount(draft.getMoveCount())
                .originalPgn(draft.getOriginalPgn())
                .metadataJson(draft.getMetadataJson())
                .summaryJson(draft.getSummaryJson())
                .moveAnalysesJson(draft.getMoveAnalysesJson())
                .aiPrompt(draft.getAiPrompt())
                .aiResponse(aiResponse)
                .feedbackMatchesJson(feedbackMatchesJson)
                .build();
    }

    public void updateFeedbackMatches(String feedbackMatchesJson) {
        this.feedbackMatchesJson = feedbackMatchesJson;
    }
}
