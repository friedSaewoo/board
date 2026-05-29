package com.example.board_test.chess.service;

import com.example.board_test.chess.dto.response.AnalysisSummaryResponse;
import com.example.board_test.chess.dto.response.GameMetadataResponse;
import com.example.board_test.chess.dto.response.MoveAnalysisResponse;
import com.example.board_test.chess.model.MoveClassification;
import com.example.board_test.chess.model.PlayerColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KoreanAiPromptServiceTest {

    private final KoreanAiPromptService service = new KoreanAiPromptService();

    @Test
    void promptContainsKoreanCoachRequestGameDataAndNoDirectAiClaim() {
        String pgn = "[Event \"Casual\"]\n1. e4 e5";
        GameMetadataResponse metadata = GameMetadataResponse.from(Map.of(
                "Event", "Casual",
                "White", "User",
                "Black", "Opponent",
                "Result", "1-0"
        ));
        AnalysisSummaryResponse summary = new AnalysisSummaryResponse(42, 1, 1, 0, 3, "중반 실수가 있었습니다.");
        List<MoveAnalysisResponse> moves = List.of(
                new MoveAnalysisResponse(1, 1, PlayerColor.WHITE, "e4", "e2e4", 20, 18, 2, MoveClassification.EXCELLENT, "e2e4", List.of("e2e4", "e7e5")),
                new MoveAnalysisResponse(3, 2, PlayerColor.WHITE, "Nf3", "g1f3", 40, -80, 120, MoveClassification.MISTAKE, "d2d4", List.of("d2d4")),
                new MoveAnalysisResponse(5, 3, PlayerColor.WHITE, "Bc4", "f1c4", 30, -260, 290, MoveClassification.BLUNDER, "d2d4", List.of("d2d4", "e5d4"))
        );

        String prompt = service.buildPrompt(pgn, PlayerColor.WHITE, metadata, summary, moves);

        assertThat(prompt)
                .contains("한국어로만 피드백")
                .contains("실전 체스 코치")
                .contains("WHITE")
                .contains("백")
                .contains("중반 실수가 있었습니다.")
                .contains("초반 수순")
                .contains("오프닝 정석 구간")
                .contains("일반적인/정석적인 오프닝 수")
                .contains("이후 미들게임 계획")
                .contains("전체 국면 균형")
                .contains("미들게임과 엔드게임/후반 후보까지 반드시 이어서")
                .contains("최대 8수")
                .contains("1. e4(e2e4)")
                .contains("핵심 장면 후보: 오프닝/미들게임/엔드게임 균형, 수순 순서")
                .contains("카테고리별로 따로 묶지 말고")
                .contains("[탁월한 수]")
                .contains("[실수]")
                .contains("[블런더]")
                .contains("missedBetter=yes")
                .contains("Nf3")
                .contains("Bc4")
                .contains("d2d4")
                .contains("핵심 수 흐름 해설")
                .contains("앱이 AI API를 직접 호출한 것이 아니라");
        assertThat(prompt)
                .doesNotContain("앱이 GPT를 호출했습니다")
                .doesNotContain("[원본 PGN]")
                .doesNotContain("[베스트/좋은 수 후보]")
                .doesNotContain("[블런더/큰 실수 후보]")
                .doesNotContain("[더 좋은 수가 있었던 장면]")
                .doesNotContain("[탁월한/베스트/좋은 수]")
                .doesNotContain("[블런더/큰 실수]")
                .doesNotContain("[놓친 더 좋은 수]")
                .doesNotContain("[Event \"Casual\"]")
                .doesNotContain("다음 1주일 훈련 추천")
                .doesNotContain("이번 판에서 반복된 패턴")
                .doesNotContain("다음 판에서 바로 신경 쓸 체크포인트");
    }

    @Test
    void promptKeepsLateMiddlegameAndEndgameCandidatesWhenOpeningHasManyCandidates() {
        GameMetadataResponse metadata = GameMetadataResponse.from(Map.of(
                "White", "User",
                "Black", "Opponent",
                "Result", "1/2-1/2"
        ));
        AnalysisSummaryResponse summary = new AnalysisSummaryResponse(88, 2, 4, 3, 71, "후반 계산이 승부를 흔들었습니다.");
        List<MoveAnalysisResponse> moves = new ArrayList<>();

        for (int ply = 1; ply <= 80; ply++) {
            PlayerColor side = ply % 2 == 1 ? PlayerColor.WHITE : PlayerColor.BLACK;
            int moveNumber = (ply + 1) / 2;
            MoveClassification classification = MoveClassification.GOOD;
            int loss = 20;
            String san = "M" + ply;
            String uci = "a2a3";
            String bestMove = "a2a3";

            if (side == PlayerColor.WHITE && ply <= 15) {
                classification = MoveClassification.MISTAKE;
                loss = 120 + ply;
                bestMove = "b2b4";
            }
            if (ply == 35) {
                classification = MoveClassification.BLUNDER;
                loss = 240;
                san = "MidBlunder";
                bestMove = "c2c4";
            }
            if (ply == 71) {
                classification = MoveClassification.BLUNDER;
                loss = 310;
                san = "LateBlunder";
                bestMove = "d2d4";
            }

            moves.add(new MoveAnalysisResponse(
                    ply,
                    moveNumber,
                    side,
                    san,
                    uci,
                    50,
                    50 - loss,
                    loss,
                    classification,
                    bestMove,
                    List.of(bestMove)
            ));
        }

        String prompt = service.buildPrompt("1. e4 e5", PlayerColor.WHITE, metadata, summary, moves);

        assertThat(prompt)
                .contains("[미들게임]")
                .contains("MidBlunder")
                .contains("[엔드게임/후반]")
                .contains("LateBlunder")
                .contains("오프닝 정석 구간 설명은 전체 답변의 20% 이하");
    }
}
