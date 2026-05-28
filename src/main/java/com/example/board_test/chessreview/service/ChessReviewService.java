package com.example.board_test.chessreview.service;

import com.example.board_test.chess.dto.response.AnalysisSummaryResponse;
import com.example.board_test.chess.dto.response.GameMetadataResponse;
import com.example.board_test.chess.dto.response.MoveAnalysisResponse;
import com.example.board_test.chessreview.dto.request.ChessReviewCreateRequest;
import com.example.board_test.chessreview.dto.request.ChessReviewMatchUpdateRequest;
import com.example.board_test.chessreview.dto.response.ChessReviewListResponse;
import com.example.board_test.chessreview.dto.response.ChessReviewResponse;
import com.example.board_test.chessreview.dto.response.FeedbackMatchResponse;
import com.example.board_test.chessreview.entity.ChessAnalysisDraft;
import com.example.board_test.chessreview.entity.ChessReview;
import com.example.board_test.chessreview.model.ChessAnalysisDraftStatus;
import com.example.board_test.chessreview.repository.ChessAnalysisDraftRepository;
import com.example.board_test.chessreview.repository.ChessReviewRepository;
import com.example.board_test.global.common.dto.page.PagedResult;
import com.example.board_test.global.exception.CustomException;
import com.example.board_test.global.exception.ErrorCode;
import com.example.board_test.member.entity.Member;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChessReviewService {

    private final ChessAnalysisDraftRepository chessAnalysisDraftRepository;
    private final ChessReviewRepository chessReviewRepository;
    private final ChessReviewJsonService jsonService;
    private final ChessReviewOwnerService ownerService;
    private final ChessAnalysisDraftService chessAnalysisDraftService;
    private final FeedbackMatchService feedbackMatchService;

    @Transactional
    public ChessReviewResponse create(String ownerEmail, ChessReviewCreateRequest request) {
        if (request.aiResponse() == null || request.aiResponse().isBlank()) {
            throw new CustomException(ErrorCode.CHESS_REVIEW_AI_RESPONSE_REQUIRED);
        }
        Member owner = ownerService.resolve(ownerEmail);
        chessAnalysisDraftService.expireOldDrafts(LocalDateTime.now());
        ChessAnalysisDraft draft = chessAnalysisDraftRepository.findByAnalysisIdAndOwnerMemberId(request.analysisId(), owner.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CHESS_ANALYSIS_DRAFT_NOT_FOUND));
        validateDraftUsable(draft);

        GameMetadataResponse metadata = jsonService.read(draft.getMetadataJson(), GameMetadataResponse.class);
        AnalysisSummaryResponse summary = jsonService.read(draft.getSummaryJson(), AnalysisSummaryResponse.class);
        List<MoveAnalysisResponse> moves = jsonService.read(draft.getMoveAnalysesJson(), new TypeReference<>() {});
        List<FeedbackMatchResponse> matches = request.matches().isEmpty()
                ? feedbackMatchService.autoMatch(request.aiResponse(), moves)
                : feedbackMatchService.validateManualMatches(request.matches(), moves);

        ChessReview review = ChessReview.fromDraft(
                draft,
                deriveTitle(metadata),
                blankToNull(metadata.white()),
                blankToNull(metadata.black()),
                blankToNull(metadata.result()),
                request.aiResponse(),
                jsonService.write(matches),
                null
        );
        ChessReview saved = chessReviewRepository.save(review);
        draft.markConverted();
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResult<ChessReviewListResponse> findAll(String ownerEmail, Pageable pageable) {
        Long ownerMemberId = ownerService.resolve(ownerEmail).getId();
        Page<ChessReview> page = chessReviewRepository.findAllByOwnerMemberId(ownerMemberId, pageable)
                .map(ChessReviewListResponse::from);
        return PagedResult.from(page);
    }

    @Transactional(readOnly = true)
    public ChessReviewResponse findById(String ownerEmail, Long id) {
        Long ownerMemberId = ownerService.resolve(ownerEmail).getId();
        ChessReview review = chessReviewRepository.findByIdAndOwnerMemberId(id, ownerMemberId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHESS_REVIEW_NOT_FOUND));
        return toResponse(review);
    }

    @Transactional
    public ChessReviewResponse updateMatches(String ownerEmail, Long id, ChessReviewMatchUpdateRequest request) {
        Long ownerMemberId = ownerService.resolve(ownerEmail).getId();
        ChessReview review = chessReviewRepository.findByIdAndOwnerMemberId(id, ownerMemberId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHESS_REVIEW_NOT_FOUND));
        List<MoveAnalysisResponse> moves = jsonService.read(review.getMoveAnalysesJson(), new TypeReference<>() {});
        List<FeedbackMatchResponse> matches = feedbackMatchService.validateManualMatches(request.matches(), moves);
        review.updateFeedbackMatches(jsonService.write(matches));
        return toResponse(review);
    }

    @Transactional
    public void delete(String ownerEmail, Long id) {
        Long ownerMemberId = ownerService.resolve(ownerEmail).getId();
        ChessReview review = chessReviewRepository.findByIdAndOwnerMemberId(id, ownerMemberId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHESS_REVIEW_NOT_FOUND));
        chessReviewRepository.delete(review);
    }

    private void validateDraftUsable(ChessAnalysisDraft draft) {
        if (draft.getStatus() == ChessAnalysisDraftStatus.CONVERTED) {
            throw new CustomException(ErrorCode.CHESS_ANALYSIS_DRAFT_CONVERTED);
        }
        if (draft.getStatus() == ChessAnalysisDraftStatus.EXPIRED || draft.isExpired(LocalDateTime.now())) {
            draft.markExpired();
            throw new CustomException(ErrorCode.CHESS_ANALYSIS_DRAFT_EXPIRED);
        }
    }

    private ChessReviewResponse toResponse(ChessReview review) {
        GameMetadataResponse metadata = jsonService.read(review.getMetadataJson(), GameMetadataResponse.class);
        AnalysisSummaryResponse summary = jsonService.read(review.getSummaryJson(), AnalysisSummaryResponse.class);
        List<MoveAnalysisResponse> moves = jsonService.read(review.getMoveAnalysesJson(), new TypeReference<>() {});
        List<FeedbackMatchResponse> feedbackMatches = jsonService.read(review.getFeedbackMatchesJson(), new TypeReference<>() {});
        return ChessReviewResponse.from(review, metadata, summary, moves, feedbackMatches);
    }

    private String deriveTitle(GameMetadataResponse metadata) {
        String event = trim(metadata.event());
        if (!event.isBlank()) {
            return truncate(event, 120);
        }
        String white = trim(metadata.white());
        String black = trim(metadata.black());
        return truncate((white.isBlank() ? "White" : white) + " vs " + (black.isBlank() ? "Black" : black), 120);
    }

    private String blankToNull(String value) {
        String trimmed = trim(value);
        return trimmed.isBlank() ? null : trimmed;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
