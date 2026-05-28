package com.example.board_test.chessreview.entity;

import com.example.board_test.chess.model.PlayerColor;
import com.example.board_test.chessreview.model.ChessAnalysisDraftStatus;
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

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "chess_analysis_drafts")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChessAnalysisDraft extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String analysisId;

    @Column(nullable = false)
    private Long ownerMemberId;

    @Column(nullable = false, length = 200)
    private String title;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PlayerColor playerColor;

    @Column(nullable = false)
    private int moveCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChessAnalysisDraftStatus status;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public static ChessAnalysisDraft create(
            String analysisId,
            Long ownerMemberId,
            String title,
            String originalPgn,
            String metadataJson,
            String summaryJson,
            String moveAnalysesJson,
            String aiPrompt,
            PlayerColor playerColor,
            int moveCount,
            LocalDateTime expiresAt
    ) {
        return ChessAnalysisDraft.builder()
                .analysisId(analysisId)
                .ownerMemberId(ownerMemberId)
                .title(title)
                .originalPgn(originalPgn)
                .metadataJson(metadataJson)
                .summaryJson(summaryJson)
                .moveAnalysesJson(moveAnalysesJson)
                .aiPrompt(aiPrompt)
                .playerColor(playerColor)
                .moveCount(moveCount)
                .status(ChessAnalysisDraftStatus.ACTIVE)
                .expiresAt(expiresAt)
                .build();
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public void markConverted() {
        this.status = ChessAnalysisDraftStatus.CONVERTED;
    }

    public void markExpired() {
        this.status = ChessAnalysisDraftStatus.EXPIRED;
    }
}
