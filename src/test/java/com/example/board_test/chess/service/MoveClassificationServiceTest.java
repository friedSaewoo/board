package com.example.board_test.chess.service;

import com.example.board_test.chess.model.EngineScore;
import com.example.board_test.chess.model.MoveClassification;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MoveClassificationServiceTest {

    private final MoveClassificationService service = new MoveClassificationService();

    @Test
    void classifiesCentipawnLossThresholds() {
        assertThat(service.classify(0)).isEqualTo(MoveClassification.BEST);
        assertThat(service.classify(20)).isEqualTo(MoveClassification.GOOD);
        assertThat(service.classify(60)).isEqualTo(MoveClassification.INACCURACY);
        assertThat(service.classify(120)).isEqualTo(MoveClassification.MISTAKE);
        assertThat(service.classify(250)).isEqualTo(MoveClassification.BLUNDER);
    }

    @Test
    void mateScoresConvertWithoutOverflowOrInversion() {
        int winningMate = EngineScore.mate(2).toCentipawnEquivalent();
        int losingMate = EngineScore.mate(-2).toCentipawnEquivalent();

        assertThat(winningMate).isPositive();
        assertThat(losingMate).isNegative();
        assertThat(winningMate - losingMate).isLessThan(Integer.MAX_VALUE);
        assertThat(service.classify(Math.max(0, winningMate - winningMate))).isEqualTo(MoveClassification.BEST);
    }
}
