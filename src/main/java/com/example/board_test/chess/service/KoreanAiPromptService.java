package com.example.board_test.chess.service;

import com.example.board_test.chess.dto.response.AnalysisSummaryResponse;
import com.example.board_test.chess.dto.response.GameMetadataResponse;
import com.example.board_test.chess.dto.response.MoveAnalysisResponse;
import com.example.board_test.chess.model.MoveClassification;
import com.example.board_test.chess.model.PlayerColor;
import org.springframework.stereotype.Service;

import java.util.List;
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
        String moveFindings = moves.stream()
                .map(move -> String.format(
                        "%d. ply %d %s %s(%s): best=%s, score %d→%d, loss=%dcp, class=%s, pv=%s",
                        move.moveNumber(),
                        move.ply(),
                        move.side().koreanName(),
                        move.san(),
                        move.uci(),
                        emptyToDash(move.bestMove()),
                        move.scoreBeforeCp(),
                        move.scoreAfterCp(),
                        move.centipawnLoss(),
                        move.classification(),
                        move.principalVariation().isEmpty() ? "-" : String.join(" ", move.principalVariation())
                ))
                .collect(Collectors.joining("\n"));

        String biggestMistakes = moves.stream()
                .filter(move -> move.side() == playerColor)
                .filter(move -> move.classification() == MoveClassification.MISTAKE || move.classification() == MoveClassification.BLUNDER)
                .map(move -> String.format("- %d수 %s: %s, 손실 %dcp", move.moveNumber(), move.side().koreanName(), move.san(), move.centipawnLoss()))
                .collect(Collectors.joining("\n"));
        if (biggestMistakes.isBlank()) {
            biggestMistakes = "- 큰 실수는 제한적이었습니다. 대신 작은 부정확성을 개선 포인트로 봐 주세요.";
        }

        return """
                당신은 실전 체스 코치입니다. 아래 PGN과 Stockfish 분석 결과를 바탕으로 한국어로만 피드백해 주세요.
                앱이 AI API를 직접 호출한 것이 아니라, 사용자가 외부 AI에 붙여넣기 위한 코칭 요청입니다.

                [코칭 관점]
                - 사용자가 둔 색: %s(%s)
                - 목표: 가장 큰 실수와 바로 개선할 후보수만 간결하게 설명
                - 길이 제한: 핵심만 700자 이내로 답변
                - 제외: 긴 수순 나열, 일반론, 연습계획, 1주일 훈련표

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

                [주요 실수/놓친 전술 후보]
                %s

                [전체 수순별 분석]
                %s

                [원본 PGN]
                %s

                [요청 출력 형식]
                1. 한줄 총평
                2. 가장 중요한 실수 최대 3개: 왜 나빴는지 + 더 나은 후보수
                3. 이번 판에서 반복된 패턴 1~2개
                4. 다음 판에서 바로 신경 쓸 체크포인트 2개
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
                biggestMistakes,
                moveFindings,
                originalPgn
        );
    }

    private String emptyToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
