package com.example.board_test.chessreview.service;

import com.example.board_test.global.exception.CustomException;
import com.example.board_test.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ChessReviewJsonService {

    private final ObjectMapper objectMapper;

    public ChessReviewJsonService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "체스 리뷰 데이터를 직렬화할 수 없습니다.");
        }
    }

    public <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "저장된 체스 리뷰 데이터를 읽을 수 없습니다.");
        }
    }

    public <T> T read(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "저장된 체스 리뷰 데이터를 읽을 수 없습니다.");
        }
    }
}
