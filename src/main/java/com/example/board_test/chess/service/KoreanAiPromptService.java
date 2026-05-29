package com.example.board_test.chess.service;

import com.example.board_test.chess.dto.response.AnalysisSummaryResponse;
import com.example.board_test.chess.dto.response.GameMetadataResponse;
import com.example.board_test.chess.dto.response.MoveAnalysisResponse;
import com.example.board_test.chess.model.MoveClassification;
import com.example.board_test.chess.model.PlayerColor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class KoreanAiPromptService {
    private static final int OPENING_CONTEXT_PLY_LIMIT = 16;
    private static final int MAX_KEY_MOVE_LINES = 24;
    private static final int OPENING_KEY_MOVE_QUOTA = 4;
    private static final int MIDDLEGAME_KEY_MOVE_QUOTA = 10;
    private static final int ENDGAME_KEY_MOVE_QUOTA = 10;

    private enum GamePhase {
        OPENING("오프닝"),
        MIDDLEGAME("미들게임"),
        ENDGAME("엔드게임/후반");

        private final String koreanName;

        GamePhase(String koreanName) {
            this.koreanName = koreanName;
        }
    }

    public String buildPrompt(
            String originalPgn,
            PlayerColor playerColor,
            GameMetadataResponse metadata,
            AnalysisSummaryResponse summary,
            List<MoveAnalysisResponse> moves
    ) {
        String keyMoveFlow = keyMoveFlowLines(moves, playerColor);
        String openingMoves = moves.stream()
                .limit(OPENING_CONTEXT_PLY_LIMIT)
                .map(move -> String.format("%d%s %s(%s)",
                        move.moveNumber(),
                        move.side() == PlayerColor.BLACK ? "..." : ".",
                        move.san(),
                        move.uci()
                ))
                .collect(Collectors.joining(" "));

        return """
                당신은 실전 체스 코치입니다. 아래 Stockfish 분석 결과를 바탕으로 한국어로만 피드백해 주세요.
                앱이 AI API를 직접 호출한 것이 아니라, 사용자가 외부 AI에 붙여넣기 위한 코칭 요청입니다.

                [코칭 관점]
                - 사용자가 둔 색: %s(%s)
                - 목표: 베스트 수, 블런더/큰 실수, 더 좋은 수가 있었지만 두지 않은 장면을 구분해서 설명
                - 설명 방식: 핵심 수는 왜 좋거나 나빴는지, 추천수가 어떤 의도인지 구체적으로 설명
                - 핵심 수 해설 순서: 베스트/블런더/놓친 수를 카테고리별로 따로 묶지 말고, 실제 수순 순서대로 설명하세요
                - 핵심 수 표기: 각 장면 제목 앞에는 [탁월한 수], [베스트 수], [좋은 수], [부정확], [실수], [블런더] 중 하나만 붙이세요
                - 전체 국면 균형: 오프닝에서 멈추지 말고 미들게임과 엔드게임/후반 후보까지 반드시 이어서 해설하세요
                - 분량 배분: 오프닝 정석 구간 설명은 전체 답변의 20%% 이하로 짧게, 나머지는 미들게임/엔드게임의 전술·전략·계산 실수에 배분하세요
                - 오프닝: chess.com처럼 어디까지가 일반적인/정석적인 오프닝 수였는지와 벗어난 첫 수/이유만 짧게 짚어 주세요
                - 오프닝 설명: 가능한 경우 오프닝 이름/구조를 추정하되, 초반 수 하나하나를 길게 강의하지 말고 이후 미들게임 계획과 어떻게 연결됐는지만 설명하세요. 확실하지 않은 이름은 단정하지 마세요
                - 후보 처리: [핵심 장면 후보]에 오프닝/미들게임/엔드게임 후보가 함께 있으면 초반 후보만 고르지 말고 각 국면에서 중요한 장면을 골고루 다루세요
                - 제외: 원본 PGN 재해석, 전체 수순 나열, 일반론, 연습계획, 1주일 훈련표

                [게임 메타데이터]
                - Event: %s
                - White: %s
                - Black: %s
                - Result: %s

                [Stockfish 요약]
                - 평균 센티폰 손실: %dcp
                - 부정확: %d, 실수: %d, 블런더: %d
                - 가장 큰 흔들림 ply: %s
                - 한줄 요약: %s

                [초반 수순: 오프닝 정석 구간 판단용 - 최대 8수]
                %s

                [핵심 장면 후보: 오프닝/미들게임/엔드게임 균형, 수순 순서]
                %s

                [요청 출력 형식]
                1. 한줄 총평
                2. 오프닝 정석 구간: 몇 수까지 일반적인 수였는지, 벗어난 첫 수/이유/이후 계획 연결을 짧게 설명
                3. 전체 핵심 수 흐름 해설: 실제 수순 순서대로 오프닝→미들게임→엔드게임/후반까지 이어서 설명하고, 카테고리별로 따로 묶지 말 것
                """.formatted(
                playerColor,
                playerColor.koreanName(),
                emptyToDash(metadata.event()),
                emptyToDash(metadata.white()),
                emptyToDash(metadata.black()),
                emptyToDash(metadata.result()),
                summary.averageCentipawnLoss(),
                summary.inaccuracies(),
                summary.mistakes(),
                summary.blunders(),
                summary.biggestSwingPly() == null ? "-" : summary.biggestSwingPly(),
                summary.headline(),
                openingMoves,
                keyMoveFlow
        );
    }

    private String keyMoveFlowLines(List<MoveAnalysisResponse> moves, PlayerColor playerColor) {
        List<MoveAnalysisResponse> playerMoves = moves.stream()
                .filter(move -> move.side() == playerColor)
                .toList();

        int totalPly = moves.stream()
                .mapToInt(MoveAnalysisResponse::ply)
                .max()
                .orElse(0);

        List<MoveAnalysisResponse> openingMoves = selectPhaseCandidates(playerMoves, GamePhase.OPENING, totalPly, OPENING_KEY_MOVE_QUOTA);
        List<MoveAnalysisResponse> middlegameMoves = selectPhaseCandidates(playerMoves, GamePhase.MIDDLEGAME, totalPly, MIDDLEGAME_KEY_MOVE_QUOTA);
        List<MoveAnalysisResponse> endgameMoves = selectPhaseCandidates(playerMoves, GamePhase.ENDGAME, totalPly, ENDGAME_KEY_MOVE_QUOTA);

        LinkedHashMap<Integer, MoveAnalysisResponse> selected = new LinkedHashMap<>();
        Stream.of(openingMoves, middlegameMoves, endgameMoves)
                .flatMap(List::stream)
                .sorted(Comparator.comparingInt(MoveAnalysisResponse::ply))
                .forEach(move -> selected.putIfAbsent(move.ply(), move));

        if (selected.size() < MAX_KEY_MOVE_LINES) {
            playerMoves.stream()
                    .filter(move -> isBlunderOrMistake(move) || isMissedBetterMove(move) || isBestOrGood(move))
                    .sorted(Comparator
                            .comparingInt(this::importanceScore)
                            .reversed()
                            .thenComparingInt(MoveAnalysisResponse::ply))
                    .limit(MAX_KEY_MOVE_LINES - selected.size())
                    .sorted(Comparator.comparingInt(MoveAnalysisResponse::ply))
                    .forEach(move -> selected.putIfAbsent(move.ply(), move));
        }

        String lines = selected.values()
                .stream()
                .sorted(Comparator.comparingInt(MoveAnalysisResponse::ply))
                .limit(MAX_KEY_MOVE_LINES)
                .map(move -> formatMoveCandidate(move, totalPly))
                .collect(Collectors.joining("\n"));
        return lines.isBlank() ? "- 해설할 핵심 장면 후보가 제한적입니다." : lines;
    }

    private List<MoveAnalysisResponse> selectPhaseCandidates(
            List<MoveAnalysisResponse> playerMoves,
            GamePhase phase,
            int totalPly,
            int quota
    ) {
        List<MoveAnalysisResponse> phaseMoves = playerMoves.stream()
                .filter(move -> phaseFor(move, totalPly) == phase)
                .toList();

        List<MoveAnalysisResponse> criticalMoves = phaseMoves.stream()
                .filter(move -> isBlunderOrMistake(move) || isMissedBetterMove(move))
                .sorted(Comparator
                        .comparingInt(this::importanceScore)
                        .reversed()
                        .thenComparingInt(MoveAnalysisResponse::ply))
                .limit(Math.max(1, quota - 2))
                .toList();

        List<MoveAnalysisResponse> goodMoves = phaseMoves.stream()
                .filter(this::isBestOrGood)
                .sorted(Comparator.comparingInt(MoveAnalysisResponse::ply))
                .limit(Math.max(1, quota - criticalMoves.size()))
                .toList();

        return Stream.concat(criticalMoves.stream(), goodMoves.stream())
                .collect(Collectors.toMap(
                        MoveAnalysisResponse::ply,
                        move -> move,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .sorted(Comparator.comparingInt(MoveAnalysisResponse::ply))
                .limit(quota)
                .toList();
    }

    private String formatMoveCandidate(MoveAnalysisResponse move, int totalPly) {
        return String.format(
                "- [%s] %d수 %s %s(%s) %s: class=%s, loss=%dcp, missedBetter=%s, best=%s, score %d→%d, pv=%s",
                phaseFor(move, totalPly).koreanName,
                move.moveNumber(),
                move.side().koreanName(),
                move.san(),
                move.uci(),
                tagsFor(move),
                move.classification(),
                move.centipawnLoss(),
                isMissedBetterMove(move) ? "yes" : "no",
                emptyToDash(move.bestMove()),
                move.scoreBeforeCp(),
                move.scoreAfterCp(),
                move.principalVariation().isEmpty() ? "-" : String.join(" ", move.principalVariation())
        );
    }

    private GamePhase phaseFor(MoveAnalysisResponse move, int totalPly) {
        if (move.ply() <= OPENING_CONTEXT_PLY_LIMIT) {
            return GamePhase.OPENING;
        }
        int endgameStartPly = Math.max(40, (int) Math.ceil(totalPly * 0.67));
        if (totalPly >= 45 && move.ply() >= endgameStartPly) {
            return GamePhase.ENDGAME;
        }
        return GamePhase.MIDDLEGAME;
    }

    private int importanceScore(MoveAnalysisResponse move) {
        int classificationWeight = switch (move.classification()) {
            case BLUNDER -> 500;
            case MISTAKE -> 350;
            case INACCURACY -> 180;
            case EXCELLENT -> 120;
            case BEST -> 100;
            case GOOD -> 80;
        };
        int missedBetterMoveWeight = isMissedBetterMove(move) ? 120 : 0;
        return classificationWeight + missedBetterMoveWeight + Math.max(0, move.centipawnLoss());
    }

    private String tagsFor(MoveAnalysisResponse move) {
        return "[" + switch (move.classification()) {
            case EXCELLENT -> "탁월한 수";
            case BEST -> "베스트 수";
            case GOOD -> "좋은 수";
            case INACCURACY -> "부정확";
            case MISTAKE -> "실수";
            case BLUNDER -> "블런더";
        } + "]";
    }

    private boolean isBestOrGood(MoveAnalysisResponse move) {
        return move.classification() == MoveClassification.BEST
                || move.classification() == MoveClassification.EXCELLENT
                || move.classification() == MoveClassification.GOOD;
    }

    private boolean isBlunderOrMistake(MoveAnalysisResponse move) {
        return move.classification() == MoveClassification.BLUNDER
                || move.classification() == MoveClassification.MISTAKE;
    }

    private boolean isMissedBetterMove(MoveAnalysisResponse move) {
        return move.centipawnLoss() > 35 && hasDifferentBestMove(move);
    }

    private boolean hasDifferentBestMove(MoveAnalysisResponse move) {
        return move.bestMove() != null
                && !move.bestMove().isBlank()
                && move.uci() != null
                && !move.bestMove().equals(move.uci());
    }

    private String emptyToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
