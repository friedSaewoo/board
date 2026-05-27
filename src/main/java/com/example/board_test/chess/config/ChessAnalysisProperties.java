package com.example.board_test.chess.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "chess.stockfish")
public class ChessAnalysisProperties {

    /** External command used to start one UCI Stockfish process per analysis request. */
    private String command = "stockfish";

    /** Default thinking time per analyzed position. */
    private long perMoveTimeoutMillis = 1000;

    /** Overall request budget; long games are rejected before engine work when the estimate exceeds it. */
    private long totalTimeoutMillis = 120_000;

    /** Maximum legal plies accepted for a single request. */
    private int maxPlies = 240;

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public long getPerMoveTimeoutMillis() {
        return perMoveTimeoutMillis;
    }

    public void setPerMoveTimeoutMillis(long perMoveTimeoutMillis) {
        this.perMoveTimeoutMillis = perMoveTimeoutMillis;
    }

    public long getTotalTimeoutMillis() {
        return totalTimeoutMillis;
    }

    public void setTotalTimeoutMillis(long totalTimeoutMillis) {
        this.totalTimeoutMillis = totalTimeoutMillis;
    }

    public int getMaxPlies() {
        return maxPlies;
    }

    public void setMaxPlies(int maxPlies) {
        this.maxPlies = maxPlies;
    }
}
