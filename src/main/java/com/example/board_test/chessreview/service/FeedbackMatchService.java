package com.example.board_test.chessreview.service;

import com.example.board_test.chess.dto.response.MoveAnalysisResponse;
import com.example.board_test.chessreview.dto.request.FeedbackMatchRequest;
import com.example.board_test.chessreview.dto.response.FeedbackMatchResponse;
import com.example.board_test.chessreview.model.FeedbackMatchConfidence;
import com.example.board_test.chessreview.model.FeedbackMatchSource;
import com.example.board_test.global.exception.CustomException;
import com.example.board_test.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FeedbackMatchService {

    private static final Pattern MOVE_REFERENCE = Pattern.compile("(?<!\\d)(\\d{1,3})(\\.\\.\\.|\\.)([A-Za-z0-9O=+#x!?-]+)");
    private static final Pattern UCI_REFERENCE = Pattern.compile("\\b([a-h][1-8][a-h][1-8][qrbn]?)\\b", Pattern.CASE_INSENSITIVE);

    public List<FeedbackMatchResponse> autoMatch(String aiResponse, List<MoveAnalysisResponse> moves) {
        List<String> segments = segment(aiResponse);
        List<FeedbackMatchResponse> matches = new ArrayList<>(segments.size());
        for (int index = 0; index < segments.size(); index++) {
            matches.add(matchSegment(index, segments.get(index), moves, FeedbackMatchSource.AUTO));
        }
        return matches;
    }

    public List<FeedbackMatchResponse> validateManualMatches(List<FeedbackMatchRequest> requests, List<MoveAnalysisResponse> moves) {
        Set<Integer> segmentIndexes = new HashSet<>();
        List<FeedbackMatchResponse> responses = new ArrayList<>(requests.size());
        for (FeedbackMatchRequest request : requests) {
            if (request == null || request.segmentIndex() < 0 || !segmentIndexes.add(request.segmentIndex())) {
                throw new CustomException(ErrorCode.CHESS_REVIEW_INVALID_MATCH);
            }
            FeedbackMatchConfidence confidence = request.confidence() == null ? FeedbackMatchConfidence.NONE : request.confidence();
            FeedbackMatchSource source = request.source() == null ? FeedbackMatchSource.MANUAL : request.source();
            if (request.matchedPly() == null) {
                responses.add(new FeedbackMatchResponse(
                        request.segmentIndex(),
                        request.text() == null ? "" : request.text(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        FeedbackMatchConfidence.NONE,
                        source
                ));
                continue;
            }
            MoveAnalysisResponse move = findByPly(moves, request.matchedPly())
                    .orElseThrow(() -> new CustomException(ErrorCode.CHESS_REVIEW_INVALID_MATCH));
            responses.add(fromMove(request.segmentIndex(), request.text() == null ? "" : request.text(), move, confidence, source));
        }
        return responses.stream()
                .sorted(Comparator.comparingInt(FeedbackMatchResponse::segmentIndex))
                .toList();
    }

    private FeedbackMatchResponse matchSegment(int segmentIndex, String text, List<MoveAnalysisResponse> moves, FeedbackMatchSource source) {
        Matcher moveMatcher = MOVE_REFERENCE.matcher(text);
        while (moveMatcher.find()) {
            int moveNumber = Integer.parseInt(moveMatcher.group(1));
            String dots = moveMatcher.group(2);
            String san = normalizeSan(moveMatcher.group(3));
            boolean black = "...".equals(dots);
            Optional<MoveAnalysisResponse> match = moves.stream()
                    .filter(move -> move.moveNumber() == moveNumber)
                    .filter(move -> black ? "BLACK".equals(move.side().name()) : "WHITE".equals(move.side().name()))
                    .filter(move -> normalizeSan(move.san()).equalsIgnoreCase(san))
                    .findFirst();
            if (match.isPresent()) {
                return fromMove(segmentIndex, text, match.get(), FeedbackMatchConfidence.HIGH, source);
            }
        }

        Matcher uciMatcher = UCI_REFERENCE.matcher(text);
        while (uciMatcher.find()) {
            String uci = uciMatcher.group(1).toLowerCase(Locale.ROOT);
            Optional<MoveAnalysisResponse> match = moves.stream()
                    .filter(move -> move.uci() != null && move.uci().equalsIgnoreCase(uci))
                    .findFirst();
            if (match.isPresent()) {
                return fromMove(segmentIndex, text, match.get(), FeedbackMatchConfidence.MEDIUM, source);
            }
        }

        return new FeedbackMatchResponse(segmentIndex, text, null, null, null, null, null, FeedbackMatchConfidence.NONE, source);
    }

    private FeedbackMatchResponse fromMove(
            int segmentIndex,
            String text,
            MoveAnalysisResponse move,
            FeedbackMatchConfidence confidence,
            FeedbackMatchSource source
    ) {
        return new FeedbackMatchResponse(
                segmentIndex,
                text,
                move.ply(),
                move.moveNumber(),
                move.side(),
                move.san(),
                move.uci(),
                confidence,
                source
        );
    }

    private Optional<MoveAnalysisResponse> findByPly(List<MoveAnalysisResponse> moves, int ply) {
        if (ply < 1 || ply > moves.size()) {
            return Optional.empty();
        }
        return moves.stream().filter(move -> move.ply() == ply).findFirst();
    }

    private List<String> segment(String aiResponse) {
        String text = aiResponse == null ? "" : aiResponse.trim();
        if (text.isBlank()) {
            return List.of();
        }
        String[] blankSplit = text.split("\\R\\s*\\R+");
        List<String> segments = Arrays.stream(blankSplit)
                .map(String::trim)
                .filter(segment -> !segment.isBlank())
                .toList();
        if (segments.size() > 1) {
            return segments;
        }
        return Arrays.stream(text.split("\\R(?=\\s*(?:#{1,6}\\s+|[-*]\\s+|\\d+[.)]\\s+))"))
                .map(String::trim)
                .filter(segment -> !segment.isBlank())
                .toList();
    }

    private String normalizeSan(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replace('0', 'O')
                .replaceAll("[!?+#]+$", "")
                .replaceAll("e\\.p\\.$", "")
                .trim();
    }
}
