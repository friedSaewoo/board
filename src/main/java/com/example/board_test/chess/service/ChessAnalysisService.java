package com.example.board_test.chess.service;

import com.example.board_test.chess.config.ChessAnalysisProperties;
import com.example.board_test.chess.dto.request.ChessAnalysisRequest;
import com.example.board_test.chess.dto.response.AnalysisSummaryResponse;
import com.example.board_test.chess.dto.response.ChessAnalysisResponse;
import com.example.board_test.chess.dto.response.GameMetadataResponse;
import com.example.board_test.chess.dto.response.MoveAnalysisResponse;
import com.example.board_test.chess.model.MoveClassification;
import com.example.board_test.chess.model.ParsedGame;
import com.example.board_test.chess.model.ParsedMove;
import com.example.board_test.chess.model.PositionEvaluation;
import com.example.board_test.chessreview.entity.ChessAnalysisDraft;
import com.example.board_test.chessreview.service.ChessAnalysisDraftService;
import com.example.board_test.chessreview.service.ChessReviewMemberService;
import com.example.board_test.global.exception.CustomException;
import com.example.board_test.global.exception.ErrorCode;
import com.example.board_test.member.entity.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ChessAnalysisService {

    private final PgnParserService pgnParserService;
    private final StockfishClient stockfishClient;
    private final MoveClassificationService moveClassificationService;
    private final KoreanAiPromptService koreanAiPromptService;
    private final ChessAnalysisProperties properties;
    private final ChessAnalysisDraftService chessAnalysisDraftService;
    private final ChessReviewMemberService chessReviewMemberService;

    @Autowired
    public ChessAnalysisService(
            PgnParserService pgnParserService,
            StockfishClient stockfishClient,
            MoveClassificationService moveClassificationService,
            KoreanAiPromptService koreanAiPromptService,
            ChessAnalysisProperties properties,
            ChessAnalysisDraftService chessAnalysisDraftService,
            ChessReviewMemberService chessReviewMemberService
    ) {
        this.pgnParserService = pgnParserService;
        this.stockfishClient = stockfishClient;
        this.moveClassificationService = moveClassificationService;
        this.koreanAiPromptService = koreanAiPromptService;
        this.properties = properties;
        this.chessAnalysisDraftService = chessAnalysisDraftService;
        this.chessReviewMemberService = chessReviewMemberService;
    }

    @Autowired
    public ChessAnalysisService(
            PgnParserService pgnParserService,
            StockfishClient stockfishClient,
            MoveClassificationService moveClassificationService,
            KoreanAiPromptService koreanAiPromptService,
            ChessAnalysisProperties properties,
            ChessAnalysisDraftService chessAnalysisDraftService,
            ChessReviewMemberService chessReviewMemberService
    ) {
        this(
                pgnParserService,
                stockfishClient,
                moveClassificationService,
                koreanAiPromptService,
                properties,
                null,
                null
        );
    }

    public ChessAnalysisResponse analyze(ChessAnalysisRequest request) {
        return buildAnalysis(request, null);
    }

    public ChessAnalysisResponse analyze(ChessAnalysisRequest request, String ownerEmail) {
        if (chessAnalysisDraftService == null || chessReviewMemberService == null) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
        Member owner = chessReviewMemberService.requireMember(ownerEmail);
        return buildAnalysis(request, owner);
    }

    private ChessAnalysisResponse buildAnalysis(ChessAnalysisRequest request, Member owner) {
        ParsedGame game = pgnParserService.parse(request.pgn());
        validateAnalysisBudget(game.moves().size());

        List<PositionEvaluation> evaluations = analyzePositions(game.moves());
        List<MoveAnalysisResponse> moveAnalyses = buildMoveAnalyses(game.moves(), evaluations);
        AnalysisSummaryResponse summary = buildSummary(moveAnalyses, request.playerColor());
        GameMetadataResponse metadata = GameMetadataResponse.from(game.headers());
        String prompt = koreanAiPromptService.buildPrompt(request.pgn(), request.playerColor(), metadata, summary, moveAnalyses);
        String analysisId = null;
        if (ownerEmail != null && !ownerEmail.isBlank() && chessAnalysisDraftService != null) {
            ChessAnalysisDraft draft = chessAnalysisDraftService.create(
                    ownerEmail,
                    request.pgn(),
                    metadata,
                    summary,
                    moveAnalyses,
                    prompt,
                    request.playerColor(),
                    game.moves().size()
            );
            analysisId = draft.getAnalysisId();
        }

        String analysisId = null;
        if (owner != null) {
            ChessAnalysisDraft draft = chessAnalysisDraftService.createDraft(
                    owner,
                    request.pgn(),
                    metadata,
                    summary,
                    moveAnalyses,
                    prompt,
                    request.playerColor()
            );
            analysisId = draft.getAnalysisId();
        }

        return new ChessAnalysisResponse(analysisId, metadata, request.playerColor(), game.moves().size(), summary, moveAnalyses, prompt);
    }

    private void validateAnalysisBudget(int plies) {
        if (plies <= 0) {
            throw new CustomException(ErrorCode.CHESS_INVALID_PGN);
        }
        if (plies > properties.getMaxPlies()) {
            throw new CustomException(ErrorCode.CHESS_ANALYSIS_LIMIT_EXCEEDED);
        }
    }

    private List<PositionEvaluation> analyzePositions(List<ParsedMove> moves) {
        List<PositionEvaluation> evaluations = new ArrayList<>(moves.size() + 1);
        List<String> appliedMoves = new ArrayList<>();
        Duration perPositionLimit = Duration.ofMillis(properties.getPerMoveTimeoutMillis());

        try (StockfishSession session = stockfishClient.startSession()) {
            for (int i = 0; i <= moves.size(); i++) {
                evaluations.add(session.analyzePosition(appliedMoves, perPositionLimit));
                if (i < moves.size()) {
                    appliedMoves.add(moves.get(i).uci());
                }
            }
        } catch (CustomException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new CustomException(ErrorCode.CHESS_STOCKFISH_UNAVAILABLE);
        }
        return evaluations;
    }

    private List<MoveAnalysisResponse> buildMoveAnalyses(List<ParsedMove> moves, List<PositionEvaluation> evaluations) {
        List<MoveAnalysisResponse> responses = new ArrayList<>(moves.size());
        for (int index = 0; index < moves.size(); index++) {
            ParsedMove move = moves.get(index);
            PositionEvaluation before = evaluations.get(index);
            PositionEvaluation after = evaluations.get(index + 1);

            int scoreBefore = before.score().toCentipawnEquivalent();
            int scoreAfterFromMoverPerspective = -after.score().toCentipawnEquivalent();
            int centipawnLoss = Math.max(0, scoreBefore - scoreAfterFromMoverPerspective);
            MoveClassification classification = moveClassificationService.classify(centipawnLoss);

            responses.add(new MoveAnalysisResponse(
                    move.ply(),
                    move.moveNumber(),
                    move.side(),
                    move.san(),
                    move.uci(),
                    scoreBefore,
                    scoreAfterFromMoverPerspective,
                    centipawnLoss,
                    classification,
                    before.bestMove(),
                    before.principalVariation()
            ));
        }
        return responses;
    }

    private AnalysisSummaryResponse buildSummary(List<MoveAnalysisResponse> moves, com.example.board_test.chess.model.PlayerColor playerColor) {
        List<MoveAnalysisResponse> playerMoves = moves.stream()
                .filter(move -> move.side() == playerColor)
                .toList();
        List<MoveAnalysisResponse> scopedMoves = playerMoves.isEmpty() ? moves : playerMoves;

        int average = (int) Math.round(scopedMoves.stream()
                .mapToInt(MoveAnalysisResponse::centipawnLoss)
                .average()
                .orElse(0));
        int inaccuracies = count(scopedMoves, MoveClassification.INACCURACY);
        int mistakes = count(scopedMoves, MoveClassification.MISTAKE);
        int blunders = count(scopedMoves, MoveClassification.BLUNDER);
        Integer biggestSwingPly = scopedMoves.stream()
                .max(Comparator.comparingInt(MoveAnalysisResponse::centipawnLoss))
                .filter(move -> move.centipawnLoss() > 0)
                .map(MoveAnalysisResponse::ply)
                .orElse(null);
        String headline = makeHeadline(blunders, mistakes, inaccuracies, average);

        return new AnalysisSummaryResponse(average, inaccuracies, mistakes, blunders, biggestSwingPly, headline);
    }

    private int count(List<MoveAnalysisResponse> moves, MoveClassification classification) {
        return (int) moves.stream()
                .filter(move -> move.classification() == classification)
                .count();
    }

    private String makeHeadline(int blunders, int mistakes, int inaccuracies, int average) {
        if (blunders > 0) {
            return "결정적인 블런더가 승부를 크게 흔들었습니다.";
        }
        if (mistakes > 0) {
            return "중요한 순간의 실수를 줄이면 안정성이 크게 올라갑니다.";
        }
        if (inaccuracies > 0) {
            return "큰 실수는 적지만 작은 부정확성을 개선할 여지가 있습니다.";
        }
        return average <= 20 ? "전반적으로 안정적인 경기였습니다." : "전술 기회를 더 정확히 계산해 보세요.";
    }
}
