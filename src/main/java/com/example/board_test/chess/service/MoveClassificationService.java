package com.example.board_test.chess.service;

import com.example.board_test.chess.model.MoveClassification;
import org.springframework.stereotype.Service;

@Service
public class MoveClassificationService {

    public MoveClassification classify(int centipawnLoss) {
        if (centipawnLoss <= 10) {
            return MoveClassification.BEST;
        }
        if (centipawnLoss <= 35) {
            return MoveClassification.GOOD;
        }
        if (centipawnLoss <= 80) {
            return MoveClassification.INACCURACY;
        }
        if (centipawnLoss <= 180) {
            return MoveClassification.MISTAKE;
        }
        return MoveClassification.BLUNDER;
    }
}
