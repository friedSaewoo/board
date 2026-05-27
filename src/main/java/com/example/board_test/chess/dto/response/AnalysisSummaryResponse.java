package com.example.board_test.chess.dto.response;

public record AnalysisSummaryResponse(
        int averageCentipawnLoss,
        int inaccuracies,
        int mistakes,
        int blunders,
        Integer biggestSwingPly,
        String headline
) {
}
