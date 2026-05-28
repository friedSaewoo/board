package com.example.board_test.chessreview.service;

import com.example.board_test.chess.dto.response.AnalysisSummaryResponse;
import com.example.board_test.chess.dto.response.GameMetadataResponse;
import com.example.board_test.chess.dto.response.MoveAnalysisResponse;
import com.example.board_test.chess.model.PlayerColor;
import com.example.board_test.chessreview.entity.ChessAnalysisDraft;
import com.example.board_test.chessreview.model.ChessAnalysisDraftStatus;
import com.example.board_test.chessreview.repository.ChessAnalysisDraftRepository;
import com.example.board_test.global.exception.CustomException;
import com.example.board_test.global.exception.ErrorCode;
import com.example.board_test.member.entity.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ChessAnalysisDraftService {

    static final int DRAFT_TTL_DAYS = 7;

    private final ChessAnalysisDraftRepository draftRepository;
    private final ChessReviewJsonService jsonService;
    private final ChessReviewTitleService titleService;
    private final Clock clock;

    public ChessAnalysisDraftService(
            ChessAnalysisDraftRepository draftRepository,
            ChessReviewJsonService jsonService,
            ChessReviewTitleService titleService,
            Clock clock
    ) {
        this.draftRepository = draftRepository;
        this.jsonService = jsonService;
        this.titleService = titleService;
        this.clock = clock;
    }

    @Transactional
    public ChessAnalysisDraft createDraft(
            Member owner,
            String originalPgn,
            GameMetadataResponse metadata,
            AnalysisSummaryResponse summary,
            List<MoveAnalysisResponse> moveAnalyses,
            String aiPrompt,
            PlayerColor playerColor
    ) {
        expireOldDrafts();
        LocalDateTime now = LocalDateTime.now(clock);
        ChessAnalysisDraft draft = ChessAnalysisDraft.create(
                UUID.randomUUID().toString(),
                owner.getId(),
                titleService.titleFrom(metadata),
                originalPgn,
                jsonService.write(metadata),
                jsonService.write(summary),
                jsonService.write(moveAnalyses),
                aiPrompt,
                playerColor,
                moveAnalyses.size(),
                now.plusDays(DRAFT_TTL_DAYS)
        );
        return draftRepository.save(draft);
    }

    @Transactional
    public int expireOldDrafts() {
        return draftRepository.markExpiredDrafts(
                ChessAnalysisDraftStatus.ACTIVE,
                ChessAnalysisDraftStatus.EXPIRED,
                LocalDateTime.now(clock)
        );
    }

    @Transactional(readOnly = true)
    public ChessAnalysisDraft requireUsableDraft(String analysisId, Long ownerMemberId) {
        ChessAnalysisDraft draft = draftRepository.findByAnalysisIdAndOwnerMemberId(analysisId, ownerMemberId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHESS_ANALYSIS_DRAFT_NOT_FOUND));
        if (draft.getStatus() == ChessAnalysisDraftStatus.CONVERTED) {
            throw new CustomException(ErrorCode.CHESS_ANALYSIS_DRAFT_ALREADY_CONVERTED);
        }
        if (draft.getStatus() == ChessAnalysisDraftStatus.EXPIRED || draft.isExpired(LocalDateTime.now(clock))) {
            throw new CustomException(ErrorCode.CHESS_ANALYSIS_DRAFT_EXPIRED);
        }
        return draft;
    }
}
