package com.example.board_test.chess.service;

import com.example.board_test.chess.model.ParsedGame;
import com.example.board_test.chess.model.ParsedMove;
import com.example.board_test.chess.model.PlayerColor;
import com.example.board_test.global.exception.CustomException;
import com.example.board_test.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PgnParserService {

    private static final Pattern HEADER_PATTERN = Pattern.compile("\\[([A-Za-z0-9_]+)\\s+\\\"([^\\\"]*)\\\"\\]");
    private static final Pattern MOVE_NUMBER_PATTERN = Pattern.compile("\\b\\d+\\.(?:\\.\\.)?");
    private static final Pattern RESULT_PATTERN = Pattern.compile("^(1-0|0-1|1/2-1/2|\\*)$");

    public ParsedGame parse(String pgn) {
        if (pgn == null || pgn.isBlank()) {
            throw new CustomException(ErrorCode.CHESS_INVALID_PGN);
        }

        try {
            Map<String, String> headers = new LinkedHashMap<>();
            Matcher matcher = HEADER_PATTERN.matcher(pgn);
            while (matcher.find()) {
                headers.put(matcher.group(1), matcher.group(2));
            }

            String moveText = matcher.replaceAll(" ");
            moveText = stripCommentsAndVariations(moveText);
            moveText = moveText.replaceAll("\\$\\d+", " ");
            moveText = MOVE_NUMBER_PATTERN.matcher(moveText).replaceAll(" ");

            BoardState board = BoardState.initial();
            List<ParsedMove> moves = new ArrayList<>();
            for (String rawToken : moveText.split("\\s+")) {
                String token = rawToken.trim();
                if (token.isBlank() || RESULT_PATTERN.matcher(token).matches() || token.equals("e.p.")) {
                    continue;
                }

                ParsedMove move = board.applySan(token, moves.size() + 1);
                moves.add(move);
            }

            if (moves.isEmpty()) {
                throw new CustomException(ErrorCode.CHESS_INVALID_PGN);
            }

            return new ParsedGame(headers, moves);
        } catch (CustomException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new CustomException(ErrorCode.CHESS_INVALID_PGN);
        }
    }

    private String stripCommentsAndVariations(String input) {
        StringBuilder output = new StringBuilder(input.length());
        int variationDepth = 0;
        boolean inBraceComment = false;
        boolean inLineComment = false;

        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);

            if (inLineComment) {
                if (ch == '\n' || ch == '\r') {
                    inLineComment = false;
                    output.append(' ');
                }
                continue;
            }

            if (inBraceComment) {
                if (ch == '}') {
                    inBraceComment = false;
                    output.append(' ');
                }
                continue;
            }

            if (variationDepth > 0) {
                if (ch == '(') {
                    variationDepth++;
                } else if (ch == ')') {
                    variationDepth--;
                    if (variationDepth == 0) {
                        output.append(' ');
                    }
                }
                continue;
            }

            if (ch == ';') {
                inLineComment = true;
                continue;
            }
            if (ch == '{') {
                inBraceComment = true;
                continue;
            }
            if (ch == '(') {
                variationDepth = 1;
                continue;
            }

            output.append(ch);
        }
        return output.toString();
    }

    private static final class BoardState {
        private static final int[][] KNIGHT_DELTAS = {
                {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
                {1, -2}, {1, 2}, {2, -1}, {2, 1}
        };
        private static final int[][] KING_DELTAS = {
                {-1, -1}, {-1, 0}, {-1, 1}, {0, -1},
                {0, 1}, {1, -1}, {1, 0}, {1, 1}
        };
        private static final int[][] BISHOP_DIRECTIONS = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        private static final int[][] ROOK_DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        private static final int[][] QUEEN_DIRECTIONS = {
                {-1, -1}, {-1, 1}, {1, -1}, {1, 1},
                {-1, 0}, {1, 0}, {0, -1}, {0, 1}
        };

        private final char[][] board;
        private PlayerColor sideToMove;
        private boolean whiteKingSideCastle;
        private boolean whiteQueenSideCastle;
        private boolean blackKingSideCastle;
        private boolean blackQueenSideCastle;
        private int enPassantRow = -1;
        private int enPassantCol = -1;

        private BoardState(char[][] board) {
            this.board = board;
        }

        static BoardState initial() {
            char[][] board = {
                    {'r', 'n', 'b', 'q', 'k', 'b', 'n', 'r'},
                    {'p', 'p', 'p', 'p', 'p', 'p', 'p', 'p'},
                    {'.', '.', '.', '.', '.', '.', '.', '.'},
                    {'.', '.', '.', '.', '.', '.', '.', '.'},
                    {'.', '.', '.', '.', '.', '.', '.', '.'},
                    {'.', '.', '.', '.', '.', '.', '.', '.'},
                    {'P', 'P', 'P', 'P', 'P', 'P', 'P', 'P'},
                    {'R', 'N', 'B', 'Q', 'K', 'B', 'N', 'R'}
            };
            BoardState state = new BoardState(board);
            state.sideToMove = PlayerColor.WHITE;
            state.whiteKingSideCastle = true;
            state.whiteQueenSideCastle = true;
            state.blackKingSideCastle = true;
            state.blackQueenSideCastle = true;
            return state;
        }

        ParsedMove applySan(String rawSan, int ply) {
            String san = cleanSan(rawSan);
            List<Move> legalMoves = legalMoves();
            Move selected;

            if (san.equals("O-O") || san.equals("0-0")) {
                selected = legalMoves.stream()
                        .filter(move -> move.castle && move.toCol == 6)
                        .findFirst()
                        .orElseThrow(() -> invalid(rawSan));
            } else if (san.equals("O-O-O") || san.equals("0-0-0")) {
                selected = legalMoves.stream()
                        .filter(move -> move.castle && move.toCol == 2)
                        .findFirst()
                        .orElseThrow(() -> invalid(rawSan));
            } else {
                selected = matchOrdinarySan(san, rawSan, legalMoves);
            }

            PlayerColor mover = sideToMove;
            int moveNumber = (ply + 1) / 2;
            String uci = selected.uci();
            applyInternal(selected, true);
            return new ParsedMove(ply, moveNumber, mover, rawSan, uci);
        }

        private Move matchOrdinarySan(String san, String rawSan, List<Move> legalMoves) {
            String normalized = san.replace("x", "");
            Character promotion = null;
            int promotionIndex = normalized.indexOf('=');
            if (promotionIndex >= 0) {
                if (promotionIndex + 1 >= normalized.length()) {
                    throw invalid(rawSan);
                }
                promotion = normalized.charAt(promotionIndex + 1);
                normalized = normalized.substring(0, promotionIndex);
            }

            if (normalized.length() < 2) {
                throw invalid(rawSan);
            }

            String destination = normalized.substring(normalized.length() - 2);
            if (!destination.matches("[a-h][1-8]")) {
                throw invalid(rawSan);
            }
            int toCol = destination.charAt(0) - 'a';
            int toRow = 8 - Character.digit(destination.charAt(1), 10);

            char piece = 'P';
            int start = 0;
            char first = normalized.charAt(0);
            if ("KQRBN".indexOf(first) >= 0) {
                piece = first;
                start = 1;
            }
            String disambiguation = normalized.substring(start, normalized.length() - 2);

            List<Move> matches = legalMoves.stream()
                    .filter(move -> Character.toUpperCase(pieceAt(move.fromRow, move.fromCol)) == piece)
                    .filter(move -> move.toRow == toRow && move.toCol == toCol)
                    .filter(move -> promotion == null ? move.promotion == 0 : move.promotion == promotion)
                    .filter(move -> matchesDisambiguation(move, disambiguation))
                    .toList();

            if (matches.size() != 1) {
                throw invalid(rawSan);
            }
            return matches.getFirst();
        }

        private boolean matchesDisambiguation(Move move, String disambiguation) {
            if (disambiguation.isBlank()) {
                return true;
            }
            for (int i = 0; i < disambiguation.length(); i++) {
                char ch = disambiguation.charAt(i);
                if (ch >= 'a' && ch <= 'h' && move.fromCol != ch - 'a') {
                    return false;
                }
                if (ch >= '1' && ch <= '8' && move.fromRow != 8 - Character.digit(ch, 10)) {
                    return false;
                }
            }
            return true;
        }

        private String cleanSan(String san) {
            String cleaned = san.trim()
                    .replace('0', 'O')
                    .replace("\u00d7", "x")
                    .replace("e.p.", "");
            while (!cleaned.isEmpty()) {
                char last = cleaned.charAt(cleaned.length() - 1);
                if (last == '+' || last == '#' || last == '!' || last == '?') {
                    cleaned = cleaned.substring(0, cleaned.length() - 1);
                } else {
                    break;
                }
            }
            return cleaned;
        }

        private List<Move> legalMoves() {
            List<Move> moves = new ArrayList<>();
            for (Move move : pseudoLegalMoves(sideToMove)) {
                BoardState copy = copy();
                copy.applyInternal(move, true);
                if (!copy.isInCheck(sideToMove)) {
                    moves.add(move);
                }
            }
            return moves;
        }

        private List<Move> pseudoLegalMoves(PlayerColor side) {
            List<Move> moves = new ArrayList<>();
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    char piece = pieceAt(row, col);
                    if (piece == '.' || colorOf(piece) != side) {
                        continue;
                    }
                    switch (Character.toUpperCase(piece)) {
                        case 'P' -> addPawnMoves(moves, side, row, col);
                        case 'N' -> addJumpMoves(moves, side, row, col, KNIGHT_DELTAS);
                        case 'B' -> addSlidingMoves(moves, side, row, col, BISHOP_DIRECTIONS);
                        case 'R' -> addSlidingMoves(moves, side, row, col, ROOK_DIRECTIONS);
                        case 'Q' -> addSlidingMoves(moves, side, row, col, QUEEN_DIRECTIONS);
                        case 'K' -> addKingMoves(moves, side, row, col);
                        default -> throw new IllegalStateException("Unexpected piece: " + piece);
                    }
                }
            }
            return moves;
        }

        private void addPawnMoves(List<Move> moves, PlayerColor side, int row, int col) {
            int direction = side == PlayerColor.WHITE ? -1 : 1;
            int startRow = side == PlayerColor.WHITE ? 6 : 1;
            int promotionRow = side == PlayerColor.WHITE ? 0 : 7;
            int oneRow = row + direction;

            if (isInside(oneRow, col) && pieceAt(oneRow, col) == '.') {
                addPawnMoveOrPromotions(moves, row, col, oneRow, col, promotionRow, false);
                int twoRow = row + 2 * direction;
                if (row == startRow && pieceAt(twoRow, col) == '.') {
                    moves.add(new Move(row, col, twoRow, col, (char) 0, false, false));
                }
            }

            for (int dc : new int[]{-1, 1}) {
                int targetCol = col + dc;
                if (!isInside(oneRow, targetCol)) {
                    continue;
                }
                char target = pieceAt(oneRow, targetCol);
                if (target != '.' && colorOf(target) == side.opposite()) {
                    addPawnMoveOrPromotions(moves, row, col, oneRow, targetCol, promotionRow, false);
                } else if (oneRow == enPassantRow && targetCol == enPassantCol) {
                    moves.add(new Move(row, col, oneRow, targetCol, (char) 0, false, true));
                }
            }
        }

        private void addPawnMoveOrPromotions(List<Move> moves, int fromRow, int fromCol, int toRow, int toCol, int promotionRow, boolean enPassant) {
            if (toRow == promotionRow) {
                for (char promotion : new char[]{'Q', 'R', 'B', 'N'}) {
                    moves.add(new Move(fromRow, fromCol, toRow, toCol, promotion, false, enPassant));
                }
            } else {
                moves.add(new Move(fromRow, fromCol, toRow, toCol, (char) 0, false, enPassant));
            }
        }

        private void addJumpMoves(List<Move> moves, PlayerColor side, int row, int col, int[][] deltas) {
            for (int[] delta : deltas) {
                int nextRow = row + delta[0];
                int nextCol = col + delta[1];
                if (isInside(nextRow, nextCol) && (pieceAt(nextRow, nextCol) == '.' || colorOf(pieceAt(nextRow, nextCol)) == side.opposite())) {
                    moves.add(new Move(row, col, nextRow, nextCol, (char) 0, false, false));
                }
            }
        }

        private void addSlidingMoves(List<Move> moves, PlayerColor side, int row, int col, int[][] directions) {
            for (int[] direction : directions) {
                int nextRow = row + direction[0];
                int nextCol = col + direction[1];
                while (isInside(nextRow, nextCol)) {
                    char target = pieceAt(nextRow, nextCol);
                    if (target == '.') {
                        moves.add(new Move(row, col, nextRow, nextCol, (char) 0, false, false));
                    } else {
                        if (colorOf(target) == side.opposite()) {
                            moves.add(new Move(row, col, nextRow, nextCol, (char) 0, false, false));
                        }
                        break;
                    }
                    nextRow += direction[0];
                    nextCol += direction[1];
                }
            }
        }

        private void addKingMoves(List<Move> moves, PlayerColor side, int row, int col) {
            addJumpMoves(moves, side, row, col, KING_DELTAS);
            addCastlingMoves(moves, side);
        }

        private void addCastlingMoves(List<Move> moves, PlayerColor side) {
            int row = side == PlayerColor.WHITE ? 7 : 0;
            char king = side == PlayerColor.WHITE ? 'K' : 'k';
            char rook = side == PlayerColor.WHITE ? 'R' : 'r';
            if (pieceAt(row, 4) != king || isInCheck(side)) {
                return;
            }
            boolean kingSide = side == PlayerColor.WHITE ? whiteKingSideCastle : blackKingSideCastle;
            if (kingSide && pieceAt(row, 7) == rook && pieceAt(row, 5) == '.' && pieceAt(row, 6) == '.'
                    && !isSquareAttacked(row, 5, side.opposite()) && !isSquareAttacked(row, 6, side.opposite())) {
                moves.add(new Move(row, 4, row, 6, (char) 0, true, false));
            }
            boolean queenSide = side == PlayerColor.WHITE ? whiteQueenSideCastle : blackQueenSideCastle;
            if (queenSide && pieceAt(row, 0) == rook && pieceAt(row, 1) == '.' && pieceAt(row, 2) == '.' && pieceAt(row, 3) == '.'
                    && !isSquareAttacked(row, 3, side.opposite()) && !isSquareAttacked(row, 2, side.opposite())) {
                moves.add(new Move(row, 4, row, 2, (char) 0, true, false));
            }
        }

        private boolean isInCheck(PlayerColor side) {
            char king = side == PlayerColor.WHITE ? 'K' : 'k';
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    if (pieceAt(row, col) == king) {
                        return isSquareAttacked(row, col, side.opposite());
                    }
                }
            }
            return true;
        }

        private boolean isSquareAttacked(int row, int col, PlayerColor bySide) {
            int pawnRow = bySide == PlayerColor.WHITE ? row + 1 : row - 1;
            char pawn = bySide == PlayerColor.WHITE ? 'P' : 'p';
            for (int dc : new int[]{-1, 1}) {
                int pawnCol = col + dc;
                if (isInside(pawnRow, pawnCol) && pieceAt(pawnRow, pawnCol) == pawn) {
                    return true;
                }
            }

            char knight = bySide == PlayerColor.WHITE ? 'N' : 'n';
            for (int[] delta : KNIGHT_DELTAS) {
                int r = row + delta[0];
                int c = col + delta[1];
                if (isInside(r, c) && pieceAt(r, c) == knight) {
                    return true;
                }
            }

            if (attackedBySlider(row, col, bySide, BISHOP_DIRECTIONS, 'B', 'Q')) {
                return true;
            }
            if (attackedBySlider(row, col, bySide, ROOK_DIRECTIONS, 'R', 'Q')) {
                return true;
            }

            char king = bySide == PlayerColor.WHITE ? 'K' : 'k';
            for (int[] delta : KING_DELTAS) {
                int r = row + delta[0];
                int c = col + delta[1];
                if (isInside(r, c) && pieceAt(r, c) == king) {
                    return true;
                }
            }
            return false;
        }

        private boolean attackedBySlider(int row, int col, PlayerColor bySide, int[][] directions, char pieceA, char pieceB) {
            for (int[] direction : directions) {
                int r = row + direction[0];
                int c = col + direction[1];
                while (isInside(r, c)) {
                    char piece = pieceAt(r, c);
                    if (piece != '.') {
                        if (colorOf(piece) == bySide) {
                            char upper = Character.toUpperCase(piece);
                            return upper == pieceA || upper == pieceB;
                        }
                        break;
                    }
                    r += direction[0];
                    c += direction[1];
                }
            }
            return false;
        }

        private void applyInternal(Move move, boolean switchTurn) {
            char moving = pieceAt(move.fromRow, move.fromCol);
            char captured = pieceAt(move.toRow, move.toCol);

            board[move.fromRow][move.fromCol] = '.';
            if (move.enPassant) {
                int capturedPawnRow = move.fromRow;
                captured = board[capturedPawnRow][move.toCol];
                board[capturedPawnRow][move.toCol] = '.';
            }

            char placed = move.promotion == 0 ? moving : (isWhitePiece(moving) ? move.promotion : Character.toLowerCase(move.promotion));
            board[move.toRow][move.toCol] = placed;

            if (move.castle) {
                if (move.toCol == 6) {
                    board[move.toRow][5] = board[move.toRow][7];
                    board[move.toRow][7] = '.';
                } else if (move.toCol == 2) {
                    board[move.toRow][3] = board[move.toRow][0];
                    board[move.toRow][0] = '.';
                }
            }

            updateCastlingRights(move, moving, captured);
            updateEnPassant(move, moving);
            if (switchTurn) {
                sideToMove = sideToMove.opposite();
            }
        }

        private void updateCastlingRights(Move move, char moving, char captured) {
            if (moving == 'K') {
                whiteKingSideCastle = false;
                whiteQueenSideCastle = false;
            } else if (moving == 'k') {
                blackKingSideCastle = false;
                blackQueenSideCastle = false;
            } else if (moving == 'R') {
                if (move.fromRow == 7 && move.fromCol == 0) whiteQueenSideCastle = false;
                if (move.fromRow == 7 && move.fromCol == 7) whiteKingSideCastle = false;
            } else if (moving == 'r') {
                if (move.fromRow == 0 && move.fromCol == 0) blackQueenSideCastle = false;
                if (move.fromRow == 0 && move.fromCol == 7) blackKingSideCastle = false;
            }

            if (captured == 'R') {
                if (move.toRow == 7 && move.toCol == 0) whiteQueenSideCastle = false;
                if (move.toRow == 7 && move.toCol == 7) whiteKingSideCastle = false;
            } else if (captured == 'r') {
                if (move.toRow == 0 && move.toCol == 0) blackQueenSideCastle = false;
                if (move.toRow == 0 && move.toCol == 7) blackKingSideCastle = false;
            }
        }

        private void updateEnPassant(Move move, char moving) {
            enPassantRow = -1;
            enPassantCol = -1;
            if (Character.toUpperCase(moving) == 'P' && Math.abs(move.toRow - move.fromRow) == 2) {
                enPassantRow = (move.fromRow + move.toRow) / 2;
                enPassantCol = move.fromCol;
            }
        }

        private BoardState copy() {
            char[][] boardCopy = new char[8][8];
            for (int row = 0; row < 8; row++) {
                System.arraycopy(board[row], 0, boardCopy[row], 0, 8);
            }
            BoardState copy = new BoardState(boardCopy);
            copy.sideToMove = sideToMove;
            copy.whiteKingSideCastle = whiteKingSideCastle;
            copy.whiteQueenSideCastle = whiteQueenSideCastle;
            copy.blackKingSideCastle = blackKingSideCastle;
            copy.blackQueenSideCastle = blackQueenSideCastle;
            copy.enPassantRow = enPassantRow;
            copy.enPassantCol = enPassantCol;
            return copy;
        }

        private RuntimeException invalid(String san) {
            return new CustomException(ErrorCode.CHESS_INVALID_PGN, "Invalid PGN move: " + san);
        }

        private char pieceAt(int row, int col) {
            return board[row][col];
        }

        private boolean isInside(int row, int col) {
            return row >= 0 && row < 8 && col >= 0 && col < 8;
        }

        private PlayerColor colorOf(char piece) {
            if (piece == '.') {
                return null;
            }
            return isWhitePiece(piece) ? PlayerColor.WHITE : PlayerColor.BLACK;
        }

        private boolean isWhitePiece(char piece) {
            return Character.isUpperCase(piece);
        }

        private record Move(int fromRow, int fromCol, int toRow, int toCol, char promotion, boolean castle, boolean enPassant) {
            String uci() {
                String value = square(fromRow, fromCol) + square(toRow, toCol);
                if (promotion != 0) {
                    value += Character.toLowerCase(promotion);
                }
                return value;
            }

            private String square(int row, int col) {
                return String.valueOf((char) ('a' + col)) + (8 - row);
            }
        }
    }
}
