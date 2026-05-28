package com.example.board_test.chess.service;

import com.example.board_test.chess.config.ChessAnalysisProperties;
import com.example.board_test.chess.dto.request.ChessAnalysisRequest;
import com.example.board_test.chess.dto.response.ChessAnalysisResponse;
import com.example.board_test.chess.model.EngineScore;
import com.example.board_test.chess.model.MoveClassification;
import com.example.board_test.chess.model.PlayerColor;
import com.example.board_test.chess.model.PositionEvaluation;
import com.example.board_test.global.exception.CustomException;
import com.example.board_test.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChessAnalysisServiceTest {

    @Test
    void analyzesEveryParsedPlyAndBuildsSummaryAndPromptWithFakeStockfish() {
        FakeStockfishClient fakeStockfish = new FakeStockfishClient(List.of(
                new PositionEvaluation(EngineScore.cp(20), "e2e4", List.of("e2e4", "e7e5")),
                new PositionEvaluation(EngineScore.cp(-18), "e7e5", List.of("e7e5")),
                new PositionEvaluation(EngineScore.cp(15), "g1f3", List.of("g1f3")),
                new PositionEvaluation(EngineScore.cp(-20), "b8c6", List.of("b8c6")),
                new PositionEvaluation(EngineScore.cp(210), "d7d5", List.of("d7d5"))
        ));
        ChessAnalysisService service = service(fakeStockfish, defaultProperties());

        ChessAnalysisResponse response = service.analyze(new ChessAnalysisRequest("[Event \"Mini\"]\n1. e4 e5 2. Nf3 Nc6", PlayerColor.BLACK));

        assertThat(response.moveCount()).isEqualTo(4);
        assertThat(response.moves()).hasSize(4);
        assertThat(response.moves()).extracting("uci")
                .containsExactly("e2e4", "e7e5", "g1f3", "b8c6");
        assertThat(fakeStockfish.calls()).containsExactly(
                List.of(),
                List.of("e2e4"),
                List.of("e2e4", "e7e5"),
                List.of("e2e4", "e7e5", "g1f3"),
                List.of("e2e4", "e7e5", "g1f3", "b8c6")
        );
        assertThat(response.playerColor()).isEqualTo(PlayerColor.BLACK);
        assertThat(response.metadata().event()).isEqualTo("Mini");
        assertThat(response.summary().averageCentipawnLoss()).isGreaterThanOrEqualTo(0);
        assertThat(response.aiPrompt())
                .contains("BLACK")
                .contains("초반 수순")
                .contains("1. e4(e2e4)")
                .contains("2... Nc6(b8c6)")
                .doesNotContain("[원본 PGN]");
    }

    @Test
    void classifiesCentipawnLossFromMoverPerspectiveIncludingMateLikeSwings() {
        FakeStockfishClient fakeStockfish = new FakeStockfishClient(List.of(
                new PositionEvaluation(EngineScore.mate(2), "f7f8q", List.of("f7f8q")),
                new PositionEvaluation(EngineScore.cp(0), "e7e5", List.of("e7e5")),
                new PositionEvaluation(EngineScore.cp(0), "g1f3", List.of("g1f3"))
        ));
        ChessAnalysisService service = service(fakeStockfish, defaultProperties());

        ChessAnalysisResponse response = service.analyze(new ChessAnalysisRequest("1. e4 e5", PlayerColor.WHITE));

        assertThat(response.moves().getFirst().centipawnLoss()).isGreaterThan(1000);
        assertThat(response.moves().getFirst().classification()).isEqualTo(MoveClassification.BLUNDER);
    }

    @Test
    void maxPlyPolicyRejectsBeforeStockfishWork() {
        ChessAnalysisProperties properties = defaultProperties();
        properties.setMaxPlies(1);
        FakeStockfishClient fakeStockfish = new FakeStockfishClient(List.of());
        ChessAnalysisService service = service(fakeStockfish, properties);

        assertThatThrownBy(() -> service.analyze(new ChessAnalysisRequest("1. e4 e5", PlayerColor.WHITE)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHESS_ANALYSIS_LIMIT_EXCEEDED);
        assertThat(fakeStockfish.started()).isFalse();
    }

    @Test
    void longGamesAreAcceptedWithoutEstimatedTotalTimeoutRejection() {
        ChessAnalysisProperties properties = defaultProperties();
        FakeStockfishClient fakeStockfish = new FakeStockfishClient(Collections.nCopies(
                151,
                new PositionEvaluation(EngineScore.cp(0), "e7e5", List.of("e7e5"))
        ));
        ChessAnalysisService service = service(fakeStockfish, properties);

        ChessAnalysisResponse response = service.analyze(new ChessAnalysisRequest("""
                [White "bakibarnez"]
                [Black "friedSaewoo"]
                [Result "1/2-1/2"]
                [WhiteElo "828"]
                [BlackElo "831"]
                [TimeControl "600"]
                1. e4 d5 2. exd5 Qxd5 3. Nc3 Qa5 4. d4 e6 5. a3 c5 6. Qe2 cxd4 7. Qb5+ Qxb5 8.
                Nxb5 Kd7 9. Bf4 f6 10. Nc7 e5 11. Nxa8 exf4 12. Nf3 Nc6 13. Bb5 a6 14. Bc4 b5
                15. Bb3 Bc5 16. O-O Bb7 17. c3 Bxa8 18. cxd4 Nxd4 19. Rfd1 Bxf3 20. gxf3 Kc6 21.
                Bf7 Ne7 22. Kg2 g5 23. Rab1 Rd8 24. b4 Bd6 25. Rxd4 Nf5 26. Rd5 Nh4+ 27. Kh3 Be7
                28. Rc1+ Kb7 29. Rxd8 Bxd8 30. Bd5+ Kb6 31. Rc6+ Kb7 32. Rxf6+ Ka7 33. Rf7+ Kb6
                34. Rxh7 Nf5 35. Kg4 Nd4 36. Rh6+ Kc7 37. Bg8 Kb7 38. h4 gxh4 39. Kxf4 Ne2+ 40.
                Ke3 Bg5+ 41. Kxe2 Bxh6 42. Be6 Kb6 43. Kf1 Bc1 44. Kg2 Bxa3 45. Kh3 Bxb4 46.
                Kxh4 a5 47. f4 Bc3 48. f5 b4 49. Kg5 a4 50. f6 Bxf6+ 51. Kxf6 b3 52. Ke5 b2 53.
                Ba2 Kc5 54. Ke4 Kb4 55. Kd3 Ka3 56. Bb1 Kb3 57. f4 a3 58. Bc2+ Ka2 59. Kc3 b1=Q
                60. Bxb1+ Kxb1 61. f5 a2 62. f6 a1=Q+ 63. Kc4 Qxf6 64. Kd3 Kb2 65. Ke4 Kc3 66.
                Kd5 Qd4+ 67. Ke6 Kc4 68. Ke7 Qe5+ 69. Kd7 Kc5 70. Kc8 Kc6 71. Kd8 Kd6 72. Kc8
                Qe6+ 73. Kb7 Kc5 74. Kb8 Qb6+ 75. Kc8 Kd6 1/2-1/2
                """, PlayerColor.BLACK));

        assertThat(response.moveCount()).isEqualTo(150);
        assertThat(fakeStockfish.started()).isTrue();
        assertThat(fakeStockfish.calls()).hasSize(151);
    }

    @Test
    void stockfishUnavailableAndTimeoutRemainControlledErrors() {
        ChessAnalysisService unavailableService = service(FakeStockfishClient.throwing(ErrorCode.CHESS_STOCKFISH_UNAVAILABLE), defaultProperties());
        ChessAnalysisService timeoutService = service(FakeStockfishClient.throwing(ErrorCode.CHESS_ANALYSIS_TIMEOUT), defaultProperties());

        assertThatThrownBy(() -> unavailableService.analyze(new ChessAnalysisRequest("1. e4 e5", PlayerColor.WHITE)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHESS_STOCKFISH_UNAVAILABLE);
        assertThatThrownBy(() -> timeoutService.analyze(new ChessAnalysisRequest("1. e4 e5", PlayerColor.WHITE)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHESS_ANALYSIS_TIMEOUT);
    }

    private ChessAnalysisService service(StockfishClient stockfishClient, ChessAnalysisProperties properties) {
        return new ChessAnalysisService(
                new PgnParserService(),
                stockfishClient,
                new MoveClassificationService(),
                new KoreanAiPromptService(),
                properties
        );
    }

    private ChessAnalysisProperties defaultProperties() {
        ChessAnalysisProperties properties = new ChessAnalysisProperties();
        properties.setPerMoveTimeoutMillis(1000);
        properties.setMaxPlies(1000);
        return properties;
    }

    private static final class FakeStockfishClient implements StockfishClient {
        private final List<PositionEvaluation> evaluations;
        private final ErrorCode errorCode;
        private final List<List<String>> calls = new ArrayList<>();
        private boolean started;

        private FakeStockfishClient(List<PositionEvaluation> evaluations) {
            this.evaluations = evaluations;
            this.errorCode = null;
        }

        private FakeStockfishClient(ErrorCode errorCode) {
            this.evaluations = List.of();
            this.errorCode = errorCode;
        }

        static FakeStockfishClient throwing(ErrorCode errorCode) {
            return new FakeStockfishClient(errorCode);
        }

        @Override
        public StockfishSession startSession() {
            started = true;
            if (errorCode != null && errorCode == ErrorCode.CHESS_STOCKFISH_UNAVAILABLE) {
                throw new CustomException(errorCode);
            }
            return new StockfishSession() {
                private int index;

                @Override
                public PositionEvaluation analyzePosition(List<String> movesUci, Duration timeLimit) {
                    calls.add(List.copyOf(movesUci));
                    if (errorCode != null) {
                        throw new CustomException(errorCode);
                    }
                    return evaluations.get(index++);
                }

                @Override
                public void close() {
                }
            };
        }

        List<List<String>> calls() {
            return calls;
        }

        boolean started() {
            return started;
        }
    }
}
