package com.example.board_test.chess.model;

public record ParsedMove(
        int ply,
        int moveNumber,
        PlayerColor side,
        String san,
        String uci
) {
}
