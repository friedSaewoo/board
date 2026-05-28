package com.example.board_test.chessreview.entity;

import com.example.board_test.chess.model.PlayerColor;
import com.example.board_test.chessreview.model.ChessAnalysisDraftStatus;
import com.example.board_test.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "chess_analysis_drafts", indexes = {
        @Index(name = "idx_chess_analysis_draft_public_id", columnList = "analysisId", unique = true),
        @Index(name = "idx_chess_analysis_draft_owner", columnList = "ownerMemberId")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChessAnalysisDraft extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String analysisId;

    @Column(nullable = false)
    private Long ownerMemberId;

    @Column(nullable = false, length = 120)
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
    @Column(nullable = false, length = 16)
    private PlayerColor playerColor;

    @Column(nullable = false)
    private int moveCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
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
        return new ChessAnalysisDraft(
                null,
                analysisId,
                ownerMemberId,
                title,
                originalPgn,
                metadataJson,
                summaryJson,
                moveAnalysesJson,
                aiPrompt,
                playerColor,
                moveCount,
                ChessAnalysisDraftStatus.ACTIVE,
                expiresAt
        );
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now) || expiresAt.isEqual(now);
    }

    public void markConverted() {
        this.status = ChessAnalysisDraftStatus.CONVERTED;
    }

    public void markExpired() {
        this.status = ChessAnalysisDraftStatus.EXPIRED;
    }
}
