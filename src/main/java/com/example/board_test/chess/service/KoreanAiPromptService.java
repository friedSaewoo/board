package com.example.board_test.chess.service;

import com.example.board_test.chess.dto.response.AnalysisSummaryResponse;
import com.example.board_test.chess.dto.response.GameMetadataResponse;
import com.example.board_test.chess.dto.response.MoveAnalysisResponse;
import com.example.board_test.chess.model.MoveClassification;
import com.example.board_test.chess.model.PlayerColor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class KoreanAiPromptService {

    public String buildPrompt(
            String originalPgn,
            PlayerColor playerColor,
            GameMetadataResponse metadata,
            AnalysisSummaryResponse summary,
            List<MoveAnalysisResponse> moves
    ) {
        String keyMoveFlow = keyMoveFlowLines(moves, playerColor);
        String openingMoves = moves.stream()
                .limit(24)
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
                - 핵심 수 표기: 각 장면 제목 앞에 [베스트/좋은 수], [블런더/큰 실수], [놓친 더 좋은 수] 중 해당 태그를 붙이세요
                - 오프닝: chess.com처럼 어디까지가 일반적인/정석적인 오프닝 수였는지, 정석에서 벗어난 첫 수와 그 이유를 자세히 짚어 주세요
                - 오프닝 설명: 가능한 경우 오프닝 이름/구조를 추정하고, 초반 수의 목적(중앙 장악, 전개, 킹 안전, 템포)을 연결해서 설명하세요. 확실하지 않은 이름은 단정하지 마세요
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

                [초반 수순: 오프닝 정석 구간 판단용 - 최대 12수]
                %s

                [핵심 장면 후보: 수순 순서]
                %s

                [요청 출력 형식]
                1. 한줄 총평
                2. 오프닝 정석 구간: 몇 수까지 일반적인 수였는지, 벗어난 첫 수/이유/초반 계획을 자세히 설명
                3. 핵심 수 흐름 해설: 실제 수순 순서대로 각 장면을 설명하고, 카테고리별로 따로 묶지 말 것
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
        List<MoveAnalysisResponse> criticalMoves = moves.stream()
                .filter(move -> move.side() == playerColor)
                .filter(move -> isBlunderOrMistake(move) || isMissedBetterMove(move))
                .toList();
        List<MoveAnalysisResponse> goodMoves = moves.stream()
                .filter(move -> move.side() == playerColor)
                .filter(this::isBestOrGood)
                .limit(6)
                .toList();

        String lines = Stream.concat(criticalMoves.stream(), goodMoves.stream())
                .collect(Collectors.toMap(
                        MoveAnalysisResponse::ply,
                        move -> move,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .sorted(Comparator.comparingInt(MoveAnalysisResponse::ply))
                .limit(12)
                .map(move -> String.format(
                        "- %d수 %s %s(%s) %s: class=%s, loss=%dcp, best=%s, score %d→%d, pv=%s",
                        move.moveNumber(),
                        move.side().koreanName(),
                        move.san(),
                        move.uci(),
                        tagsFor(move),
                        move.classification(),
                        move.centipawnLoss(),
                        emptyToDash(move.bestMove()),
                        move.scoreBeforeCp(),
                        move.scoreAfterCp(),
                        move.principalVariation().isEmpty() ? "-" : String.join(" ", move.principalVariation())
                ))
                .collect(Collectors.joining("\n"));
        return lines.isBlank() ? "- 해설할 핵심 장면 후보가 제한적입니다." : lines;
    }

    private String tagsFor(MoveAnalysisResponse move) {
        List<String> tags = new ArrayList<>();
        if (isBestOrGood(move)) {
            tags.add("베스트/좋은 수");
        }
        if (isBlunderOrMistake(move)) {
            tags.add("블런더/큰 실수");
        }
        if (isMissedBetterMove(move)) {
            tags.add("놓친 더 좋은 수");
        }
        return tags.stream()
                .map(tag -> "[" + tag + "]")
                .collect(Collectors.joining(""));
    }

    private boolean isBestOrGood(MoveAnalysisResponse move) {
        return move.classification() == MoveClassification.BEST
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
