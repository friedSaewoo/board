package com.example.board_test.chess.service;

import com.example.board_test.chess.dto.response.AnalysisSummaryResponse;
import com.example.board_test.chess.dto.response.GameMetadataResponse;
import com.example.board_test.chess.dto.response.MoveAnalysisResponse;
import com.example.board_test.chess.model.MoveClassification;
import com.example.board_test.chess.model.PlayerColor;
import org.junit.jupiter.api.Test;

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
                new MoveAnalysisResponse(1, 1, PlayerColor.WHITE, "e4", "e2e4", 20, 18, 2, MoveClassification.GOOD, "e2e4", List.of("e2e4", "e7e5")),
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
                .contains("초반 수의 목적")
                .contains("최대 12수")
                .contains("1. e4(e2e4)")
                .contains("베스트/좋은 수 후보")
                .contains("블런더/큰 실수 후보")
                .contains("더 좋은 수가 있었던 장면")
                .contains("Nf3")
                .contains("Bc4")
                .contains("d2d4")
                .contains("핵심 수 해설")
                .contains("앱이 AI API를 직접 호출한 것이 아니라");
        assertThat(prompt)
                .doesNotContain("앱이 GPT를 호출했습니다")
                .doesNotContain("[원본 PGN]")
                .doesNotContain("[Event \"Casual\"]")
                .doesNotContain("오프닝/미들게임/엔드게임")
                .doesNotContain("다음 1주일 훈련 추천")
                .doesNotContain("이번 판에서 반복된 패턴")
                .doesNotContain("다음 판에서 바로 신경 쓸 체크포인트");
    }
}
