package com.example.board_test.chessreview.service;

import com.example.board_test.chess.dto.response.AnalysisSummaryResponse;
import com.example.board_test.chess.dto.response.GameMetadataResponse;
import com.example.board_test.chess.dto.response.MoveAnalysisResponse;
import com.example.board_test.chess.model.PlayerColor;
import com.example.board_test.chessreview.entity.ChessAnalysisDraft;
import com.example.board_test.chessreview.model.ChessAnalysisDraftStatus;
import com.example.board_test.chessreview.repository.ChessAnalysisDraftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChessAnalysisDraftService {

    private static final int DRAFT_TTL_DAYS = 7;

    private final ChessAnalysisDraftRepository chessAnalysisDraftRepository;
    private final ChessReviewJsonService jsonService;
    private final ChessReviewOwnerService ownerService;

    @Transactional
    public ChessAnalysisDraft create(
            String ownerEmail,
            String originalPgn,
            GameMetadataResponse metadata,
            AnalysisSummaryResponse summary,
            List<MoveAnalysisResponse> moveAnalyses,
            String aiPrompt,
            PlayerColor playerColor,
            int moveCount
    ) {
        expireOldDrafts(LocalDateTime.now());
        Long ownerMemberId = ownerService.resolve(ownerEmail).getId();
        ChessAnalysisDraft draft = ChessAnalysisDraft.create(
                UUID.randomUUID().toString(),
                ownerMemberId,
                deriveTitle(metadata),
                originalPgn,
                jsonService.write(metadata),
                jsonService.write(summary),
                jsonService.write(moveAnalyses),
                aiPrompt,
                playerColor,
                moveCount,
                LocalDateTime.now().plusDays(DRAFT_TTL_DAYS)
        );
        return chessAnalysisDraftRepository.save(draft);
    }

    @Transactional
    public void expireOldDrafts(LocalDateTime now) {
        chessAnalysisDraftRepository.findAllByStatusAndExpiresAtBefore(ChessAnalysisDraftStatus.ACTIVE, now)
                .forEach(ChessAnalysisDraft::markExpired);
    }

    private String deriveTitle(GameMetadataResponse metadata) {
        String event = trim(metadata.event());
        if (!event.isBlank()) {
            return event.substring(0, Math.min(120, event.length()));
        }
        String white = trim(metadata.white());
        String black = trim(metadata.black());
        String title = (white.isBlank() ? "White" : white) + " vs " + (black.isBlank() ? "Black" : black);
        return title.substring(0, Math.min(120, title.length()));
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
