package com.example.board_test.chessreview.service;

import com.example.board_test.chess.dto.response.MoveAnalysisResponse;
import com.example.board_test.chessreview.dto.request.FeedbackMatchRequest;
import com.example.board_test.chessreview.dto.response.FeedbackMatchResponse;
import com.example.board_test.chessreview.model.FeedbackMatchConfidence;
import com.example.board_test.chessreview.model.FeedbackMatchSource;
import com.example.board_test.global.exception.CustomException;
import com.example.board_test.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FeedbackMatchService {

    private static final Pattern LEADING_MOVE_REFERENCE = Pattern.compile(
            "(?iu)(?:^|[^\\p{Alnum}])([1-9]\\d*)\\.(\\.\\.)?\\s*([O0]-[O0](?:-[O0])?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?|[a-h][1-8](?:=[QRBN])?)[+#?!]*"
    );
    private static final Pattern UCI_REFERENCE = Pattern.compile("(?i)\\b([a-h][1-8][a-h][1-8][qrbn]?)\\b");

    public List<FeedbackMatchResponse> autoMatch(String aiResponse, List<MoveAnalysisResponse> moves) {
        List<String> segments = segment(aiResponse);
        List<FeedbackMatchResponse> responses = new ArrayList<>(segments.size());
        for (int index = 0; index < segments.size(); index++) {
            String text = segments.get(index);
            MoveAnalysisResponse move = findReferencedMove(text, moves);
            responses.add(toResponse(index, text, move, confidenceFor(move), FeedbackMatchSource.AUTO));
        }
        return responses;
    }

    public List<FeedbackMatchResponse> normalizeManualMatches(List<FeedbackMatchRequest> requests, List<MoveAnalysisResponse> moves) {
        if (requests == null) {
            return List.of();
        }
        Set<Integer> segmentIndexes = new HashSet<>();
        List<FeedbackMatchResponse> responses = new ArrayList<>(requests.size());
        for (FeedbackMatchRequest request : requests) {
            if (request.segmentIndex() == null || request.segmentIndex() < 0 || !segmentIndexes.add(request.segmentIndex())) {
                throw invalidMatch();
            }

            MoveAnalysisResponse move = null;
            if (request.matchedPly() != null) {
                move = moveByPly(request.matchedPly(), moves);
                if (move == null) {
                    throw invalidMatch();
                }
            }

            FeedbackMatchSource source = request.source() == null ? FeedbackMatchSource.MANUAL : request.source();
            FeedbackMatchConfidence confidence = request.confidence() == null
                    ? (move == null ? FeedbackMatchConfidence.NONE : FeedbackMatchConfidence.HIGH)
                    : request.confidence();

            if (move == null && confidence != FeedbackMatchConfidence.NONE) {
                confidence = FeedbackMatchConfidence.NONE;
            }

            responses.add(toResponse(
                    request.segmentIndex(),
                    request.text() == null ? "" : request.text(),
                    move,
                    confidence,
                    source
            ));
        }
        return responses;
    }

    private List<String> segment(String aiResponse) {
        if (aiResponse == null || aiResponse.isBlank()) {
            return List.of();
        }
        String[] rawSegments = aiResponse.strip().split("(?:\\r?\\n){2,}|(?m)^\\s*(?=#{1,6}\\s+)");
        List<String> segments = new ArrayList<>();
        for (String rawSegment : rawSegments) {
            String text = rawSegment.trim();
            if (!text.isBlank()) {
                segments.add(text);
            }
        }
        if (segments.isEmpty()) {
            segments.add(aiResponse.strip());
        }
        return segments;
    }

    private MoveAnalysisResponse findReferencedMove(String text, List<MoveAnalysisResponse> moves) {
        Matcher sanMatcher = LEADING_MOVE_REFERENCE.matcher(text);
        while (sanMatcher.find()) {
            int moveNumber = Integer.parseInt(sanMatcher.group(1));
            boolean black = sanMatcher.group(2) != null;
            String san = normalizeSan(sanMatcher.group(3));
            MoveAnalysisResponse matched = moves.stream()
                    .filter(move -> move.moveNumber() == moveNumber)
                    .filter(move -> black ? move.side().name().equals("BLACK") : move.side().name().equals("WHITE"))
                    .filter(move -> normalizeSan(move.san()).equals(san))
                    .findFirst()
                    .orElse(null);
            if (matched != null) {
                return matched;
            }
        }

        Matcher uciMatcher = UCI_REFERENCE.matcher(text);
        while (uciMatcher.find()) {
            String uci = uciMatcher.group(1).toLowerCase();
            MoveAnalysisResponse matched = moves.stream()
                    .filter(move -> move.uci() != null && move.uci().equalsIgnoreCase(uci))
                    .findFirst()
                    .orElse(null);
            if (matched != null) {
                return matched;
            }
        }
        return null;
    }

    private MoveAnalysisResponse moveByPly(int ply, List<MoveAnalysisResponse> moves) {
        return moves.stream()
                .filter(move -> move.ply() == ply)
                .findFirst()
                .orElse(null);
    }

    private FeedbackMatchResponse toResponse(
            int segmentIndex,
            String text,
            MoveAnalysisResponse move,
            FeedbackMatchConfidence confidence,
            FeedbackMatchSource source
    ) {
        return new FeedbackMatchResponse(
                segmentIndex,
                text,
                move == null ? null : move.ply(),
                move == null ? null : move.moveNumber(),
                move == null ? null : move.side(),
                move == null ? null : move.san(),
                move == null ? null : move.uci(),
                confidence,
                source
        );
    }

    private FeedbackMatchConfidence confidenceFor(MoveAnalysisResponse move) {
        return move == null ? FeedbackMatchConfidence.NONE : FeedbackMatchConfidence.HIGH;
    }

    private String normalizeSan(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replace('0', 'O')
                .replaceAll("[+#?!]+$", "")
                .replace("x", "")
                .toUpperCase();
    }

    private CustomException invalidMatch() {
        return new CustomException(ErrorCode.CHESS_REVIEW_INVALID_MATCH);
    }
}
