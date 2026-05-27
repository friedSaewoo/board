package com.example.board_test.chess.model;

public record EngineScore(Integer centipawns, Integer mateIn) {
    private static final int MATE_BASE = 100_000;
    private static final int MATE_STEP = 100;

    public static EngineScore cp(int centipawns) {
        return new EngineScore(centipawns, null);
    }

    public static EngineScore mate(int mateIn) {
        return new EngineScore(null, mateIn);
    }

    public int toCentipawnEquivalent() {
        if (centipawns != null) {
            return centipawns;
        }
        if (mateIn == null || mateIn == 0) {
            return 0;
        }
        int distancePenalty = Math.min(Math.abs(mateIn) * MATE_STEP, MATE_BASE / 2);
        return mateIn > 0 ? MATE_BASE - distancePenalty : -MATE_BASE + distancePenalty;
    }

    public String display() {
        if (mateIn != null) {
            return "M" + mateIn;
        }
        return String.valueOf(centipawns == null ? 0 : centipawns);
    }
}
