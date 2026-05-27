package com.example.board_test.chess.service;

import com.example.board_test.chess.config.ChessAnalysisProperties;
import com.example.board_test.global.exception.CustomException;
import com.example.board_test.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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

    @Test
    void bareStockfishCommandFallsBackToExecutableCandidate(@TempDir Path tempDir) throws Exception {
        Path stockfish = tempDir.resolve("stockfish");
        Files.writeString(stockfish, "#!/bin/sh\n");
        assertThat(stockfish.toFile().setExecutable(true)).isTrue();

        List<String> fallback = ExternalProcessStockfishClient.fallbackCommand(
                List.of("stockfish", "--option"),
                List.of(tempDir.resolve("missing-stockfish"), stockfish)
        );

        assertThat(fallback).containsExactly(stockfish.toString(), "--option");
    }

    @Test
    void explicitStockfishPathDoesNotFallback(@TempDir Path tempDir) throws Exception {
        Path stockfish = tempDir.resolve("stockfish");
        Files.writeString(stockfish, "#!/bin/sh\n");
        assertThat(stockfish.toFile().setExecutable(true)).isTrue();

        List<String> fallback = ExternalProcessStockfishClient.fallbackCommand(
                List.of("/custom/stockfish"),
                List.of(stockfish)
        );

        assertThat(fallback).isEmpty();
    }
}
