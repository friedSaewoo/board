package com.example.board_test.chess.service;

import com.example.board_test.chess.model.PositionEvaluation;

import java.time.Duration;
import java.util.List;

public interface StockfishSession extends AutoCloseable {
    PositionEvaluation analyzePosition(List<String> movesUci, Duration timeLimit);

    @Override
    void close();
}
