package com.example.board_test.chess.service;

import com.example.board_test.chess.dto.response.AnalysisSummaryResponse;
import com.example.board_test.chess.dto.response.GameMetadataResponse;
import com.example.board_test.chess.dto.response.MoveAnalysisResponse;
import com.example.board_test.chess.model.MoveClassification;
import com.example.board_test.chess.model.PlayerColor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class KoreanAiPromptService {

    public String buildPrompt(
            String originalPgn,
            PlayerColor playerColor,
            GameMetadataResponse metadata,
            AnalysisSummaryResponse summary,
            List<MoveAnalysisResponse> moves
    ) {
        String bestMoves = candidateLines(
                moves,
                playerColor,
                move -> move.classification() == MoveClassification.BEST || move.classification() == MoveClassification.GOOD,
                "- 베스트/좋은 수로 분류된 수는 제한적입니다."
        );
        String blunders = candidateLines(
                moves,
                playerColor,
                move -> move.classification() == MoveClassification.BLUNDER || move.classification() == MoveClassification.MISTAKE,
                "- 블런더나 큰 실수로 분류된 수는 제한적입니다."
        );
        String missedBetterMoves = candidateLines(
                moves,
                playerColor,
                move -> move.centipawnLoss() > 35 && hasDifferentBestMove(move),
                "- 엔진 추천수와 크게 갈린 장면은 제한적입니다."
        );
        String openingMoves = moves.stream()
                .limit(16)
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
                - 오프닝: chess.com처럼 어디까지가 일반적인/정석적인 오프닝 수였는지, 정석에서 벗어난 첫 수가 보이면 짚어 주세요
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

                [초반 수순: 오프닝 정석 구간 판단용]
                %s

                [베스트/좋은 수 후보]
                %s

                [블런더/큰 실수 후보]
                %s

                [더 좋은 수가 있었던 장면]
                %s

                [요청 출력 형식]
                1. 한줄 총평
                2. 오프닝 정석 구간: 몇 수까지 일반적인 수였는지, 벗어난 첫 수가 있다면 짧게 설명
                3. 핵심 수 해설: 베스트 수 / 블런더 / 놓친 더 좋은 수를 구분해서 설명
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
                bestMoves,
                blunders,
                missedBetterMoves
        );
    }

    private String candidateLines(
            List<MoveAnalysisResponse> moves,
            PlayerColor playerColor,
            Predicate<MoveAnalysisResponse> filter,
            String fallback
    ) {
        String lines = moves.stream()
                .filter(move -> move.side() == playerColor)
                .filter(filter)
                .limit(6)
                .map(move -> String.format(
                        "- %d수 %s %s(%s): class=%s, loss=%dcp, best=%s, score %d→%d, pv=%s",
                        move.moveNumber(),
                        move.side().koreanName(),
                        move.san(),
                        move.uci(),
                        move.classification(),
                        move.centipawnLoss(),
                        emptyToDash(move.bestMove()),
                        move.scoreBeforeCp(),
                        move.scoreAfterCp(),
                        move.principalVariation().isEmpty() ? "-" : String.join(" ", move.principalVariation())
                ))
                .collect(Collectors.joining("\n"));
        return lines.isBlank() ? fallback : lines;
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
