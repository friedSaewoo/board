package com.example.board_test.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST,"COMMON_001","올바르지 않은 입력값입니다."),

    // Board
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND,"BOARD_001","해당 게시판을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
