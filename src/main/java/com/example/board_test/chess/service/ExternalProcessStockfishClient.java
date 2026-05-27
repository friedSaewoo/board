package com.example.board_test.chess.service;

import com.example.board_test.chess.config.ChessAnalysisProperties;
import com.example.board_test.chess.model.EngineScore;
import com.example.board_test.chess.model.PositionEvaluation;
import com.example.board_test.global.exception.CustomException;
import com.example.board_test.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
public class ExternalProcessStockfishClient implements StockfishClient {
    private static final List<Path> STOCKFISH_FALLBACK_PATHS = List.of(
            Path.of("/usr/games/stockfish"),
            Path.of("/usr/local/bin/stockfish"),
            Path.of("/opt/homebrew/bin/stockfish")
    );

    private final ChessAnalysisProperties properties;

    public ExternalProcessStockfishClient(ChessAnalysisProperties properties) {
        this.properties = properties;
    }

    @Override
    public StockfishSession startSession() {
        List<String> command = Arrays.stream(properties.getCommand().trim().split("\\s+"))
                .filter(part -> !part.isBlank())
                .toList();
        if (command.isEmpty()) {
            throw new CustomException(ErrorCode.CHESS_STOCKFISH_UNAVAILABLE);
        }

        return startSession(command, true);
    }

    private StockfishSession startSession(List<String> command, boolean allowFallback) {
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            UciStockfishSession session = new UciStockfishSession(process);
            session.initialize();
            return session;
        } catch (IOException e) {
            if (allowFallback) {
                List<String> fallbackCommand = fallbackCommand(command, STOCKFISH_FALLBACK_PATHS);
                if (!fallbackCommand.isEmpty()) {
                    return startSession(fallbackCommand, false);
                }
            }
            throw new CustomException(ErrorCode.CHESS_STOCKFISH_UNAVAILABLE);
        }
    }

    static List<String> fallbackCommand(List<String> command, List<Path> candidates) {
        if (command.isEmpty() || !"stockfish".equals(command.getFirst())) {
            return List.of();
        }
        return candidates.stream()
                .filter(Files::isExecutable)
                .findFirst()
                .map(path -> {
                    List<String> fallback = new ArrayList<>(command);
                    fallback.set(0, path.toString());
                    return List.copyOf(fallback);
                })
                .orElse(List.of());
    }

    static final class UciStockfishSession implements StockfishSession {
        private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(5);
        private final Process process;
        private final BufferedWriter writer;
        private final LinkedBlockingQueue<String> lines = new LinkedBlockingQueue<>();
        private volatile boolean closed;

        UciStockfishSession(Process process) {
            this.process = process;
            this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            Thread readerThread = new Thread(this::readOutput, "stockfish-uci-reader");
            readerThread.setDaemon(true);
            readerThread.start();
        }

        void initialize() {
            send("uci");
            readUntil("uciok", STARTUP_TIMEOUT, ErrorCode.CHESS_STOCKFISH_UNAVAILABLE);
            send("isready");
            readUntil("readyok", STARTUP_TIMEOUT, ErrorCode.CHESS_STOCKFISH_UNAVAILABLE);
        }

        @Override
        public PositionEvaluation analyzePosition(List<String> movesUci, Duration timeLimit) {
            ensureAlive();
            lines.clear();
            String position = "position startpos";
            if (movesUci != null && !movesUci.isEmpty()) {
                position += " moves " + String.join(" ", movesUci);
            }
            send(position);
            send("go movetime " + Math.max(1, timeLimit.toMillis()));

            long deadline = System.nanoTime() + timeLimit.plusMillis(750).toNanos();
            EngineScore score = EngineScore.cp(0);
            List<String> pv = List.of();
            String bestMove = "";

            while (true) {
                String line = pollLine(deadline, ErrorCode.CHESS_ANALYSIS_TIMEOUT);
                if (line.startsWith("info ")) {
                    ParsedInfo parsed = parseInfoLine(line);
                    if (parsed.score() != null) {
                        score = parsed.score();
                    }
                    if (!parsed.principalVariation().isEmpty()) {
                        pv = parsed.principalVariation();
                    }
                    continue;
                }
                if (line.startsWith("bestmove")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2 && !"(none)".equals(parts[1])) {
                        bestMove = parts[1];
                    }
                    return new PositionEvaluation(score, bestMove, pv);
                }
            }
        }

        static ParsedInfo parseInfoLine(String line) {
            String[] parts = line.trim().split("\\s+");
            EngineScore score = null;
            List<String> pv = List.of();
            for (int i = 0; i < parts.length; i++) {
                if ("score".equals(parts[i]) && i + 2 < parts.length) {
                    try {
                        if ("cp".equals(parts[i + 1])) {
                            score = EngineScore.cp(Integer.parseInt(parts[i + 2]));
                        } else if ("mate".equals(parts[i + 1])) {
                            score = EngineScore.mate(Integer.parseInt(parts[i + 2]));
                        }
                    } catch (NumberFormatException ignored) {
                        score = EngineScore.cp(0);
                    }
                }
                if ("pv".equals(parts[i]) && i + 1 < parts.length) {
                    pv = new ArrayList<>(Arrays.asList(parts).subList(i + 1, parts.length));
                    break;
                }
            }
            return new ParsedInfo(score, pv);
        }

        private void readUntil(String marker, Duration timeout, ErrorCode timeoutCode) {
            long deadline = System.nanoTime() + timeout.toNanos();
            while (true) {
                String line = pollLine(deadline, timeoutCode);
                if (line.equals(marker)) {
                    return;
                }
            }
        }

        private String pollLine(long deadlineNanos, ErrorCode timeoutCode) {
            ensureAlive();
            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0) {
                close();
                throw new CustomException(timeoutCode);
            }
            try {
                String line = lines.poll(remainingNanos, TimeUnit.NANOSECONDS);
                if (line == null) {
                    close();
                    throw new CustomException(timeoutCode);
                }
                return line;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                close();
                throw new CustomException(ErrorCode.CHESS_ANALYSIS_TIMEOUT);
            }
        }

        private void send(String command) {
            ensureAlive();
            try {
                writer.write(command);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                close();
                throw new CustomException(ErrorCode.CHESS_STOCKFISH_UNAVAILABLE);
            }
        }

        private void ensureAlive() {
            if (closed || !process.isAlive()) {
                throw new CustomException(ErrorCode.CHESS_STOCKFISH_UNAVAILABLE);
            }
        }

        private void readOutput() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.offer(line);
                }
            } catch (IOException ignored) {
                // analyzePosition/startup convert missing output into controlled timeout/unavailable errors.
            }
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            try {
                writer.write("quit");
                writer.newLine();
                writer.flush();
            } catch (IOException ignored) {
                // Best-effort cleanup.
            }
            process.destroy();
            try {
                if (!process.waitFor(500, TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }

        record ParsedInfo(EngineScore score, List<String> principalVariation) {
            ParsedInfo {
                principalVariation = principalVariation == null ? List.of() : List.copyOf(principalVariation);
            }
        }
    }
}
