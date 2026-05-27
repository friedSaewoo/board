package com.example.board_test.chess.service;

import com.example.board_test.chess.config.ChessAnalysisProperties;
import com.example.board_test.global.exception.CustomException;
import com.example.board_test.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalProcessStockfishClientTest {

    @Test
    void parsesCentipawnScorePrincipalVariationAndBestMoveData() {
        ExternalProcessStockfishClient.UciStockfishSession.ParsedInfo parsed =
                ExternalProcessStockfishClient.UciStockfishSession.parseInfoLine("info depth 12 score cp -34 nodes 200 pv e2e4 e7e5 g1f3");

        assertThat(parsed.score().centipawns()).isEqualTo(-34);
        assertThat(parsed.score().mateIn()).isNull();
        assertThat(parsed.principalVariation()).containsExactly("e2e4", "e7e5", "g1f3");
    }

    @Test
    void parsesMateScoreWithoutOverflow() {
        ExternalProcessStockfishClient.UciStockfishSession.ParsedInfo parsed =
                ExternalProcessStockfishClient.UciStockfishSession.parseInfoLine("info depth 8 score mate -3 pv h7h8q");

        assertThat(parsed.score().mateIn()).isEqualTo(-3);
        assertThat(parsed.score().toCentipawnEquivalent()).isNegative();
        assertThat(Math.abs(parsed.score().toCentipawnEquivalent())).isLessThan(Integer.MAX_VALUE);
    }

    @Test
    void missingCommandMapsToControlledUnavailableError() {
        ChessAnalysisProperties properties = new ChessAnalysisProperties();
        properties.setCommand("definitely-missing-stockfish-command-omx-test");
        ExternalProcessStockfishClient client = new ExternalProcessStockfishClient(properties);

        assertThatThrownBy(client::startSession)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHESS_STOCKFISH_UNAVAILABLE);
    }
}
