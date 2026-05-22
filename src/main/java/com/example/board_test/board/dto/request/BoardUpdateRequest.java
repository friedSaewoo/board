package com.example.board_test.board.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class BoardUpdateRequest {
    @NotNull
    private String title;
    @NotNull
    private String contents;
}
