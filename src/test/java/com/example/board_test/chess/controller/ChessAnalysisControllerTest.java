package com.example.board_test.chess.controller;

import com.example.board_test.chess.dto.request.ChessAnalysisRequest;
import com.example.board_test.chess.dto.response.AnalysisSummaryResponse;
import com.example.board_test.chess.dto.response.ChessAnalysisResponse;
import com.example.board_test.chess.dto.response.GameMetadataResponse;
import com.example.board_test.chess.dto.response.MoveAnalysisResponse;
import com.example.board_test.chess.model.MoveClassification;
import com.example.board_test.chess.model.PlayerColor;
import com.example.board_test.chess.service.ChessAnalysisService;
import com.example.board_test.global.exception.CustomException;
import com.example.board_test.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ChessAnalysisControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ChessAnalysisService chessAnalysisService;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void analyzeRequiresAuthenticationBeforeBroadPermitAllMatcher() throws Exception {
        mockMvc.perform(post("/chess/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChessAnalysisRequest("1. e4 e5", PlayerColor.WHITE))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validAuthenticatedRequestReturnsAnalysisContract() throws Exception {
        when(chessAnalysisService.analyze(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/chess/analyze")
                        .with(user("player@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChessAnalysisRequest("1. e4 e5", PlayerColor.WHITE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.event").value("Casual"))
                .andExpect(jsonPath("$.playerColor").value("WHITE"))
                .andExpect(jsonPath("$.moveCount").value(2))
                .andExpect(jsonPath("$.moves", hasSize(2)))
                .andExpect(jsonPath("$.moves[0].san").value("e4"))
                .andExpect(jsonPath("$.summary.averageCentipawnLoss").value(10))
                .andExpect(jsonPath("$.aiPrompt").value("한국어 코칭 프롬프트"));
    }

    @Test
    @WithMockUser
    void blankPgnReturnsValidationErrorResponse() throws Exception {
        mockMvc.perform(post("/chess/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pgn\":\"\",\"playerColor\":\"WHITE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @WithMockUser
    void missingPlayerColorReturnsValidationErrorResponse() throws Exception {
        mockMvc.perform(post("/chess/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pgn\":\"1. e4 e5\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    @WithMockUser
    void malformedPgnReturnsControlledChessError() throws Exception {
        when(chessAnalysisService.analyze(any())).thenThrow(new CustomException(ErrorCode.CHESS_INVALID_PGN));

        mockMvc.perform(post("/chess/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChessAnalysisRequest("1. nope", PlayerColor.WHITE))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHESS_001"));
    }

    @Test
    @WithMockUser
    void unavailableStockfishReturnsServiceUnavailableError() throws Exception {
        when(chessAnalysisService.analyze(any())).thenThrow(new CustomException(ErrorCode.CHESS_STOCKFISH_UNAVAILABLE));

        mockMvc.perform(post("/chess/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChessAnalysisRequest("1. e4 e5", PlayerColor.WHITE))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("CHESS_002"));
    }

    private ChessAnalysisResponse sampleResponse() {
        return new ChessAnalysisResponse(
                GameMetadataResponse.from(Map.of("Event", "Casual", "White", "User", "Black", "Opponent", "Result", "1-0")),
                PlayerColor.WHITE,
                2,
                new AnalysisSummaryResponse(10, 0, 0, 0, 1, "안정적인 경기였습니다."),
                List.of(
                        new MoveAnalysisResponse(1, 1, PlayerColor.WHITE, "e4", "e2e4", 20, 18, 2, MoveClassification.GOOD, "e2e4", List.of("e2e4")),
                        new MoveAnalysisResponse(2, 1, PlayerColor.BLACK, "e5", "e7e5", -18, -15, 0, MoveClassification.BEST, "e7e5", List.of("e7e5"))
                ),
                "한국어 코칭 프롬프트"
        );
    }
}
