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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChessReviewService {

    private final ChessAnalysisDraftRepository draftRepository;
    private final ChessReviewRepository reviewRepository;
    private final ChessReviewMemberService memberService;
    private final ChessReviewJsonService jsonService;
    private final FeedbackMatchService feedbackMatchService;
    private final Clock clock;

    public ChessReviewService(
            ChessAnalysisDraftRepository draftRepository,
            ChessReviewRepository reviewRepository,
            ChessReviewMemberService memberService,
            ChessReviewJsonService jsonService,
            FeedbackMatchService feedbackMatchService,
            Clock clock
    ) {
        this.draftRepository = draftRepository;
        this.reviewRepository = reviewRepository;
        this.memberService = memberService;
        this.jsonService = jsonService;
        this.feedbackMatchService = feedbackMatchService;
        this.clock = clock;
    }

    @Transactional
    public ChessReviewResponse create(String ownerEmail, ChessReviewCreateRequest request) {
        Member owner = memberService.requireMember(ownerEmail);
        ChessAnalysisDraft draft = draftRepository
                .findLockedByAnalysisIdAndOwnerMemberId(request.analysisId(), owner.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CHESS_ANALYSIS_DRAFT_NOT_FOUND));
        requireActiveDraft(draft);

        List<MoveAnalysisResponse> moves = readMoves(draft.getMoveAnalysesJson());
        GameMetadataResponse metadata = jsonService.read(draft.getMetadataJson(), GameMetadataResponse.class);
        AnalysisSummaryResponse summary = jsonService.read(draft.getSummaryJson(), AnalysisSummaryResponse.class);
        List<FeedbackMatchResponse> matches = request.matches() == null || request.matches().isEmpty()
                ? feedbackMatchService.autoMatch(request.aiResponse(), moves)
                : feedbackMatchService.normalizeManualMatches(request.matches(), moves);

        ChessReview review = ChessReview.fromDraft(
                draft,
                emptyToNull(metadata.white()),
                emptyToNull(metadata.black()),
                emptyToNull(metadata.result()),
                request.aiResponse().trim(),
                jsonService.write(matches)
        );
        ChessReview savedReview = reviewRepository.save(review);
        draft.markConverted();

        return toResponse(savedReview, metadata, summary, moves, matches);
    }

    @Transactional(readOnly = true)
    public PagedResult<ChessReviewListResponse> findAll(String ownerEmail, Pageable pageable) {
        Member owner = memberService.requireMember(ownerEmail);
        Page<ChessReviewListResponse> reviews = reviewRepository.findAllByOwnerMemberId(owner.getId(), pageable)
                .map(ChessReviewListResponse::from);
        return PagedResult.from(reviews);
    }

    @Transactional(readOnly = true)
    public ChessReviewResponse findById(String ownerEmail, long reviewId) {
        Member owner = memberService.requireMember(ownerEmail);
        ChessReview review = reviewRepository.findByIdAndOwnerMemberId(reviewId, owner.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CHESS_REVIEW_NOT_FOUND));
        return toResponse(review);
    }

    @Transactional
    public ChessReviewResponse updateMatches(String ownerEmail, long reviewId, ChessReviewMatchUpdateRequest request) {
        Member owner = memberService.requireMember(ownerEmail);
        ChessReview review = reviewRepository.findByIdAndOwnerMemberId(reviewId, owner.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CHESS_REVIEW_NOT_FOUND));
        List<MoveAnalysisResponse> moves = readMoves(review.getMoveAnalysesJson());
        List<FeedbackMatchResponse> matches = feedbackMatchService.normalizeManualMatches(request.matches(), moves);
        review.updateFeedbackMatches(jsonService.write(matches));
        return toResponse(review, matches);
    }

    @Transactional
    public void delete(String ownerEmail, long reviewId) {
        Member owner = memberService.requireMember(ownerEmail);
        ChessReview review = reviewRepository.findByIdAndOwnerMemberId(reviewId, owner.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CHESS_REVIEW_NOT_FOUND));
        reviewRepository.delete(review);
    }

    private void requireActiveDraft(ChessAnalysisDraft draft) {
        if (draft.getStatus() == ChessAnalysisDraftStatus.CONVERTED) {
            throw new CustomException(ErrorCode.CHESS_ANALYSIS_DRAFT_ALREADY_CONVERTED);
        }
        if (draft.getStatus() == ChessAnalysisDraftStatus.EXPIRED || draft.isExpired(LocalDateTime.now(clock))) {
            draft.markExpired();
            throw new CustomException(ErrorCode.CHESS_ANALYSIS_DRAFT_EXPIRED);
        }
        if (draft.getStatus() != ChessAnalysisDraftStatus.ACTIVE) {
            throw new CustomException(ErrorCode.CHESS_ANALYSIS_DRAFT_NOT_FOUND);
        }
    }

    private ChessReviewResponse toResponse(ChessReview review) {
        return toResponse(review, readMatches(review.getFeedbackMatchesJson()));
    }

    private ChessReviewResponse toResponse(ChessReview review, List<FeedbackMatchResponse> matches) {
        return toResponse(
                review,
                jsonService.read(review.getMetadataJson(), GameMetadataResponse.class),
                jsonService.read(review.getSummaryJson(), AnalysisSummaryResponse.class),
                readMoves(review.getMoveAnalysesJson()),
                matches
        );
    }

    private ChessReviewResponse toResponse(
            ChessReview review,
            GameMetadataResponse metadata,
            AnalysisSummaryResponse summary,
            List<MoveAnalysisResponse> moves,
            List<FeedbackMatchResponse> matches
    ) {
        return ChessReviewResponse.from(review, metadata, summary, moves, matches);
    }

    private List<MoveAnalysisResponse> readMoves(String moveAnalysesJson) {
        return jsonService.read(moveAnalysesJson, new TypeReference<List<MoveAnalysisResponse>>() {
        });
    }

    private List<FeedbackMatchResponse> readMatches(String matchesJson) {
        return jsonService.read(matchesJson, new TypeReference<List<FeedbackMatchResponse>>() {
        });
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
