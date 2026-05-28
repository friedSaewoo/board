package com.example.board_test.chess.controller;

import com.example.board_test.chess.dto.request.ChessAnalysisRequest;
import com.example.board_test.chess.dto.response.ChessAnalysisResponse;
import com.example.board_test.chess.service.ChessAnalysisService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chess")
public class ChessAnalysisController {

    private final ChessAnalysisService chessAnalysisService;

    public ChessAnalysisController(ChessAnalysisService chessAnalysisService) {
        this.chessAnalysisService = chessAnalysisService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<ChessAnalysisResponse> analyze(
            @Valid @RequestBody ChessAnalysisRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(chessAnalysisService.analyze(request, authentication.getName()));
    }
}
