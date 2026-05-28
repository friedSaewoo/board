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
    void totalTimeoutPolicyRejectsBeforeStockfishWork() {
        ChessAnalysisProperties properties = defaultProperties();
        properties.setTotalTimeoutMillis(1000);
        properties.setPerMoveTimeoutMillis(1000);
        FakeStockfishClient fakeStockfish = new FakeStockfishClient(List.of());
        ChessAnalysisService service = service(fakeStockfish, properties);

        assertThatThrownBy(() -> service.analyze(new ChessAnalysisRequest("1. e4 e5", PlayerColor.WHITE)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHESS_ANALYSIS_LIMIT_EXCEEDED);
        assertThat(fakeStockfish.started()).isFalse();
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
        properties.setTotalTimeoutMillis(120_000);
        properties.setMaxPlies(240);
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
