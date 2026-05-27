package com.example.board_test.chess.dto.response;

import java.util.Map;

public record GameMetadataResponse(
        String event,
        String site,
        String date,
        String round,
        String white,
        String black,
        String result,
        Map<String, String> headers
) {
    public GameMetadataResponse {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public static GameMetadataResponse from(Map<String, String> headers) {
        return new GameMetadataResponse(
                headers.getOrDefault("Event", ""),
                headers.getOrDefault("Site", ""),
                headers.getOrDefault("Date", ""),
                headers.getOrDefault("Round", ""),
                headers.getOrDefault("White", ""),
                headers.getOrDefault("Black", ""),
                headers.getOrDefault("Result", ""),
                headers
        );
    }
}
