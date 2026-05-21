package com.example.board_test.board.controller;

import com.example.board_test.board.dto.request.BoardCreateRequest;
import com.example.board_test.board.dto.response.BoardResponse;
import com.example.board_test.board.service.BoardService;
import com.example.board_test.global.common.dto.page.PageQuery;
import com.example.board_test.global.common.dto.page.PagedResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @PostMapping
    public ResponseEntity<BoardResponse> create(@Valid @RequestBody BoardCreateRequest boardCreateRequest) {
        return ResponseEntity.ok(boardService.create(boardCreateRequest));
    }

    @GetMapping
    public ResponseEntity<PagedResult<BoardResponse>> findAll(@Valid PageQuery pageQuery) {
        return ResponseEntity.ok(boardService.findAll(pageQuery.toPageable()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoardResponse> findById(@PathVariable long id) {
        return ResponseEntity.ok(boardService.findById(id));
    }
}
