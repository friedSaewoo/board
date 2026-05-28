package com.example.board_test.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST,"COMMON_001","잘못된 입력값입니다."),

    // Member
    EMAIL_ALREADY_EXISTS(HttpStatus.BAD_REQUEST,"MEMBER_001","이미 존재하는 이메일입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND,"MEMBER_002","존재하지 않는 유저입니다."),
    // Board
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND,"BOARD_001","게시글을 찾을 수 없습니다."),

    // Chess
    CHESS_INVALID_PGN(HttpStatus.BAD_REQUEST, "CHESS_001", "유효한 PGN을 입력해 주세요."),
    CHESS_STOCKFISH_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "CHESS_002", "Stockfish 엔진을 사용할 수 없습니다."),
    CHESS_ANALYSIS_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "CHESS_003", "체스 분석 시간이 초과되었습니다."),
    CHESS_ANALYSIS_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "CHESS_004", "허용된 최대 수 제한을 초과했습니다."),
    CHESS_ANALYSIS_DRAFT_NOT_FOUND(HttpStatus.NOT_FOUND, "CHESS_005", "체스 분석 초안을 찾을 수 없습니다."),
    CHESS_ANALYSIS_DRAFT_EXPIRED(HttpStatus.GONE, "CHESS_006", "체스 분석 초안이 만료되었습니다."),
    CHESS_ANALYSIS_DRAFT_ALREADY_CONVERTED(HttpStatus.CONFLICT, "CHESS_007", "이미 리뷰로 변환된 체스 분석 초안입니다."),
    CHESS_REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "CHESS_008", "체스 리뷰를 찾을 수 없습니다."),
    CHESS_REVIEW_INVALID_MATCH(HttpStatus.BAD_REQUEST, "CHESS_009", "AI 피드백 매칭 정보가 올바르지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
