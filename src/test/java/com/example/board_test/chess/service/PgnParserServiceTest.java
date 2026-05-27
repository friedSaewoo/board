package com.example.board_test.chess.service;

import com.example.board_test.chess.model.ParsedGame;
import com.example.board_test.global.exception.CustomException;
import com.example.board_test.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PgnParserServiceTest {

    private final PgnParserService parser = new PgnParserService();

    @Test
    void parsesHeadersAndAllLegalMoves() {
        ParsedGame game = parser.parse("""
                [Event \"Casual Game\"]
                [White \"User\"]
                [Black \"Opponent\"]
                [Result \"*\"]

                1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 *
                """);

        assertThat(game.headers()).containsEntry("Event", "Casual Game");
        assertThat(game.moves()).hasSize(6);
        assertThat(game.moves().get(0).san()).isEqualTo("e4");
        assertThat(game.moves().get(0).uci()).isEqualTo("e2e4");
        assertThat(game.moves().get(5).uci()).isEqualTo("a7a6");
    }

    @Test
    void parsesMovesWithoutHeadersAndIgnoresCommentsNagsAndResult() {
        ParsedGame game = parser.parse("1. e4 {king pawn} e5 $1 2. Nf3 Nc6 1/2-1/2");

        assertThat(game.headers()).isEmpty();
        assertThat(game.moves()).extracting("uci")
                .containsExactly("e2e4", "e7e5", "g1f3", "b8c6");
    }

    @Test
    void emptyPgnThrowsControlledInvalidPgnError() {
        assertThatThrownBy(() -> parser.parse("   "))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHESS_INVALID_PGN);
    }

    @Test
    void malformedMoveThrowsControlledInvalidPgnError() {
        assertThatThrownBy(() -> parser.parse("1. e4 e5 2. NotAMove"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHESS_INVALID_PGN);
    }
}
