package com.example.board_test.chessreview.service;

import com.example.board_test.chess.dto.response.GameMetadataResponse;
import org.springframework.stereotype.Component;

@Component
public class ChessReviewTitleService {

    public String titleFrom(GameMetadataResponse metadata) {
        if (metadata.event() != null && !metadata.event().isBlank() && !"?".equals(metadata.event().trim())) {
            return truncate(metadata.event().trim());
        }

        String white = playerName(metadata.white(), "White");
        String black = playerName(metadata.black(), "Black");
        return truncate(white + " vs " + black);
    }

    private String playerName(String value, String fallback) {
        return value == null || value.isBlank() || "?".equals(value.trim()) ? fallback : value.trim();
    }

    private String truncate(String value) {
        return value.length() <= 200 ? value : value.substring(0, 200);
    }
}
