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
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND,"BOARD_001","게시글을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
