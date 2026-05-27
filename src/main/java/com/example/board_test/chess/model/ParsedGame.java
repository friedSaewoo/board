package com.example.board_test.chess.model;

import java.util.List;
import java.util.Map;

public record ParsedGame(Map<String, String> headers, List<ParsedMove> moves) {
    public ParsedGame {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        moves = moves == null ? List.of() : List.copyOf(moves);
    }
}
