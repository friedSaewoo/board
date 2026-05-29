package com.example.board_test.chess.service;

import com.example.board_test.chess.model.MoveClassification;
import org.springframework.stereotype.Service;

@Service
public class MoveClassificationService {
    private static final double EXCELLENT_EXPECTED_POINTS_LOSS = 0.001;
    private static final double BEST_EXPECTED_POINTS_LOSS = 0.02;
    private static final double GOOD_EXPECTED_POINTS_LOSS = 0.05;
    private static final double INACCURACY_EXPECTED_POINTS_LOSS = 0.10;
    private static final double MISTAKE_EXPECTED_POINTS_LOSS = 0.20;
    private static final int EXACT_MOVE_CENTIPAWN_LOSS = 0;
    private static final int EXPECTED_POINTS_SCORE_CLAMP = 1000;
    private static final double EXPECTED_POINTS_SLOPE = 0.00368208;

    public MoveClassification classify(int scoreBeforeCp, int scoreAfterCp, int centipawnLoss) {
        double expectedPointsLoss = Math.max(0, expectedPoints(scoreBeforeCp) - expectedPoints(scoreAfterCp));

        if (centipawnLoss <= EXACT_MOVE_CENTIPAWN_LOSS && expectedPointsLoss <= EXCELLENT_EXPECTED_POINTS_LOSS) {
            return MoveClassification.EXCELLENT;
        }
        if (expectedPointsLoss <= BEST_EXPECTED_POINTS_LOSS) {
            return MoveClassification.BEST;
        }
        if (expectedPointsLoss <= GOOD_EXPECTED_POINTS_LOSS) {
            return MoveClassification.GOOD;
        }
        if (expectedPointsLoss <= INACCURACY_EXPECTED_POINTS_LOSS) {
            return MoveClassification.INACCURACY;
        }
        if (expectedPointsLoss <= MISTAKE_EXPECTED_POINTS_LOSS) {
            return MoveClassification.MISTAKE;
        }
        return MoveClassification.BLUNDER;
    }

    public MoveClassification classify(int centipawnLoss) {
        if (centipawnLoss <= EXACT_MOVE_CENTIPAWN_LOSS) {
            return MoveClassification.EXCELLENT;
        }
        if (centipawnLoss <= 25) {
            return MoveClassification.BEST;
        }
        if (centipawnLoss <= 50) {
            return MoveClassification.GOOD;
        }
        if (centipawnLoss <= 100) {
            return MoveClassification.INACCURACY;
        }
        if (centipawnLoss <= 200) {
            return MoveClassification.MISTAKE;
        }
        return MoveClassification.BLUNDER;
    }

    private double expectedPoints(int centipawns) {
        int clamped = Math.max(-EXPECTED_POINTS_SCORE_CLAMP, Math.min(EXPECTED_POINTS_SCORE_CLAMP, centipawns));
        return 1.0 / (1.0 + Math.exp(-EXPECTED_POINTS_SLOPE * clamped));
    }
}
