package com.example.board_test.chessreview.controller;

import com.example.board_test.chessreview.dto.request.ChessReviewCreateRequest;
import com.example.board_test.chessreview.dto.request.ChessReviewMatchUpdateRequest;
import com.example.board_test.chessreview.dto.response.ChessReviewListResponse;
import com.example.board_test.chessreview.dto.response.ChessReviewResponse;
import com.example.board_test.chessreview.service.ChessReviewService;
import com.example.board_test.global.common.dto.page.PageQuery;
import com.example.board_test.global.common.dto.page.PagedResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chess/reviews")
@RequiredArgsConstructor
public class ChessReviewController {

    private final ChessReviewService chessReviewService;

    @GetMapping
    public ResponseEntity<PagedResult<ChessReviewListResponse>> findAll(@Valid PageQuery pageQuery, Authentication authentication) {
        return ResponseEntity.ok(chessReviewService.findAll(authentication.getName(), pageQuery.toPageable()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChessReviewResponse> findById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(chessReviewService.findById(authentication.getName(), id));
    }

    @PostMapping
    public ResponseEntity<ChessReviewResponse> create(@Valid @RequestBody ChessReviewCreateRequest request, Authentication authentication) {
        return ResponseEntity.ok(chessReviewService.create(authentication.getName(), request));
    }

    @PatchMapping("/{id}/matches")
    public ResponseEntity<ChessReviewResponse> updateMatches(
            @PathVariable Long id,
            @Valid @RequestBody ChessReviewMatchUpdateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(chessReviewService.updateMatches(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        chessReviewService.delete(authentication.getName(), id);
        return ResponseEntity.ok().build();
    }
}
