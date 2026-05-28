package com.example.board_test.chessreview.controller;

import com.example.board_test.board.repository.BoardRepository;
import com.example.board_test.chess.model.EngineScore;
import com.example.board_test.chess.model.PlayerColor;
import com.example.board_test.chess.model.PositionEvaluation;
import com.example.board_test.chess.service.StockfishClient;
import com.example.board_test.chess.service.StockfishSession;
import com.example.board_test.member.entity.Member;
import com.example.board_test.member.repository.MemberRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ChessReviewControllerContractTest {

    private static final String SAMPLE_PGN = """
            [Event \"Contract Game\"]
            [White \"Alice\"]
            [Black \"Bob\"]
            [Result \"1-0\"]

            1. e4 e5 2. Nf3 Nc6 1-0
            """;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BoardRepository boardRepository;

    @MockitoBean
    private StockfishClient stockfishClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        Mockito.reset(stockfishClient);
        when(stockfishClient.startSession()).thenAnswer(invocation -> stockfishSession());
    }

    @Test
    void analyzeCreatesOwnerBoundDraftWithoutGenericBoardPostThenReviewCrudRestoresSavedData() throws Exception {
        String ownerEmail = createMemberEmail("owner");
        long boardCountBefore = boardRepository.count();

        JsonNode analysis = analyze(ownerEmail);
        String analysisId = requiredText(analysis, "analysisId");
        assertThat(analysisId).hasSizeGreaterThanOrEqualTo(20);
        assertThat(analysis.path("metadata").path("event").asText()).isEqualTo("Contract Game");
        assertThat(analysis.path("moves")).hasSize(4);
        assertThat(boardRepository.count()).isEqualTo(boardCountBefore);

        String aiResponse = "1...e5? 응수 이후 흑의 구조를 다시 확인하세요.\n\n2. Nf3! 개발은 자연스럽습니다.";
        JsonNode created = createReview(ownerEmail, analysisId, aiResponse);
        long reviewId = requiredLong(created, "reviewId", "id");
        assertThat(requiredText(created, "aiResponse")).isEqualTo(aiResponse);
        assertThat(requiredText(created, "playerColor")).isEqualTo("WHITE");
        assertThat(created.path("moveCount").asInt()).isEqualTo(4);
        assertThat(firstExisting(created, "feedbackMatches", "matches").isArray()).isTrue();

        JsonNode list = readJson(mockMvc.perform(get("/chess/reviews")
                        .with(user(ownerEmail))
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("sortBy", "id")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode content = firstExisting(list, "content", "reviews");
        assertThat(content.isArray()).isTrue();
        assertThat(content).anySatisfy(item -> assertThat(requiredLong(item, "reviewId", "id")).isEqualTo(reviewId));

        JsonNode detail = readJson(mockMvc.perform(get("/chess/reviews/{id}", reviewId)
                        .with(user(ownerEmail)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(requiredText(detail, "originalPgn")).contains("Contract Game");
        assertThat(requiredText(detail, "aiPrompt")).contains("초반 수순");
        assertThat(firstExisting(detail, "moves", "moveAnalyses")).hasSize(4);
        verify(stockfishClient, atLeastOnce()).startSession();
        Mockito.verifyNoMoreInteractions(stockfishClient);

        JsonNode patched = patchMatches(ownerEmail, reviewId, """
                {
                  "matches": [
                    {
                      "segmentIndex": 0,
                      "text": "수동으로 첫 수에 연결",
                      "matchedPly": 1,
                      "confidence": "HIGH",
                      "source": "MANUAL"
                    }
                  ]
                }
                """);
        JsonNode patchedMatches = firstExisting(patched, "feedbackMatches", "matches");
        assertThat(patchedMatches).hasSize(1);
        assertThat(patchedMatches.get(0).path("source").asText()).isEqualTo("MANUAL");
        assertThat(patchedMatches.get(0).path("matchedPly").asInt()).isEqualTo(1);
        assertThat(patchedMatches.get(0).path("matchedSan").asText()).isEqualTo("e4");
        assertThat(patchedMatches.get(0).path("matchedUci").asText()).isEqualTo("e2e4");

        mockMvc.perform(delete("/chess/reviews/{id}", reviewId)
                        .with(user(ownerEmail)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/chess/reviews/{id}", reviewId)
                        .with(user(ownerEmail)))
                .andExpect(status().isNotFound());
    }

    @Test
    void reviewCreationRejectsBlankUnknownAndForeignAnalysisDrafts() throws Exception {
        String ownerEmail = createMemberEmail("draft-owner");
        String otherEmail = createMemberEmail("draft-other");
        String analysisId = requiredText(analyze(ownerEmail), "analysisId");

        mockMvc.perform(post("/chess/reviews")
                        .with(user(ownerEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"analysisId":"%s","aiResponse":"   "}
                                """.formatted(analysisId)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/chess/reviews")
                        .with(user(ownerEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"analysisId":"missing-analysis-id","aiResponse":"AI response"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/chess/reviews")
                        .with(user(otherEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"analysisId":"%s","aiResponse":"다른 사용자의 분석으로 리뷰 생성 시도"}
                                """.formatted(analysisId)))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(403, 404));
    }

    @Test
    void reviewEndpointsRequireAuthenticationAndHideOtherOwnersReviews() throws Exception {
        String ownerEmail = createMemberEmail("owner-sec");
        String otherEmail = createMemberEmail("other-sec");
        long reviewId = requiredLong(createReview(ownerEmail, requiredText(analyze(ownerEmail), "analysisId"), "1...e5? 확인"), "reviewId", "id");

        mockMvc.perform(get("/chess/reviews"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/chess/reviews/{id}", reviewId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/chess/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/chess/reviews/{id}/matches", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matches\":[]}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/chess/reviews/{id}", reviewId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/chess/reviews/{id}", reviewId)
                        .with(user(otherEmail)))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(403, 404));
        mockMvc.perform(patch("/chess/reviews/{id}/matches", reviewId)
                        .with(user(otherEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matches\":[]}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(403, 404));
        mockMvc.perform(delete("/chess/reviews/{id}", reviewId)
                        .with(user(otherEmail)))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(403, 404));

        JsonNode otherList = readJson(mockMvc.perform(get("/chess/reviews")
                        .with(user(otherEmail)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(firstExisting(otherList, "content", "reviews")).isEmpty();
    }

    @Test
    void malformedMatchUpdatesAreRejectedAndDoNotMutateSavedMatches() throws Exception {
        String ownerEmail = createMemberEmail("match-owner");
        long reviewId = requiredLong(createReview(ownerEmail, requiredText(analyze(ownerEmail), "analysisId"), "1...e5? 확인"), "reviewId", "id");
        JsonNode before = readJson(mockMvc.perform(get("/chess/reviews/{id}", reviewId)
                        .with(user(ownerEmail)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode beforeMatches = firstExisting(before, "feedbackMatches", "matches");

        List<String> invalidPayloads = List.of(
                """
                        {"matches":[{"segmentIndex":-1,"text":"bad","matchedPly":1,"confidence":"HIGH","source":"MANUAL"}]}
                        """,
                """
                        {"matches":[{"segmentIndex":0,"text":"bad","matchedPly":99,"confidence":"HIGH","source":"MANUAL"}]}
                        """,
                """
                        {"matches":[{"segmentIndex":0,"text":"bad","matchedPly":1,"confidence":"UNKNOWN","source":"MANUAL"}]}
                        """,
                """
                        {"matches":[{"segmentIndex":0,"text":"a","matchedPly":1,"confidence":"HIGH","source":"MANUAL"},{"segmentIndex":0,"text":"b","matchedPly":2,"confidence":"LOW","source":"MANUAL"}]}
                        """
        );

        for (String payload : invalidPayloads) {
            mockMvc.perform(patch("/chess/reviews/{id}/matches", reviewId)
                            .with(user(ownerEmail))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());
        }

        JsonNode after = readJson(mockMvc.perform(get("/chess/reviews/{id}", reviewId)
                        .with(user(ownerEmail)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(firstExisting(after, "feedbackMatches", "matches")).isEqualTo(beforeMatches);
    }

    private JsonNode analyze(String email) throws Exception {
        return readJson(mockMvc.perform(post("/chess/analyze")
                        .with(user(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnalyzeRequest(SAMPLE_PGN, PlayerColor.WHITE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisId").isString())
                .andExpect(jsonPath("$.metadata.event").value("Contract Game"))
                .andExpect(jsonPath("$.moves").isArray())
                .andReturn());
    }

    private JsonNode createReview(String email, String analysisId, String aiResponse) throws Exception {
        MvcResult result = mockMvc.perform(post("/chess/reviews")
                        .with(user(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"analysisId":"%s","aiResponse":"%s"}
                                """.formatted(analysisId, aiResponse.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n"))))
                .andExpect(response -> assertThat(response.getResponse().getStatus()).isIn(200, 201))
                .andReturn();
        return readJson(result);
    }

    private JsonNode patchMatches(String email, long reviewId, String payload) throws Exception {
        return readJson(mockMvc.perform(patch("/chess/reviews/{id}/matches", reviewId)
                        .with(user(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn());
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String createMemberEmail(String prefix) {
        String email = prefix + "-" + UUID.randomUUID() + "@example.com";
        memberRepository.save(Member.from(prefix, email, "password"));
        return email;
    }

    private StockfishSession stockfishSession() {
        List<PositionEvaluation> evaluations = List.of(
                new PositionEvaluation(EngineScore.cp(20), "e2e4", List.of("e2e4", "e7e5")),
                new PositionEvaluation(EngineScore.cp(-18), "e7e5", List.of("e7e5")),
                new PositionEvaluation(EngineScore.cp(15), "g1f3", List.of("g1f3")),
                new PositionEvaluation(EngineScore.cp(-20), "b8c6", List.of("b8c6")),
                new PositionEvaluation(EngineScore.cp(10), "d2d4", List.of("d2d4"))
        );
        return new StockfishSession() {
            private int index;

            @Override
            public PositionEvaluation analyzePosition(List<String> movesUci, Duration timeLimit) {
                return evaluations.get(index++);
            }

            @Override
            public void close() {
            }
        };
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        assertThat(value.isTextual()).as("%s should be textual in %s", field, node).isTrue();
        return value.asText();
    }

    private long requiredLong(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isNumber()) {
                return value.asLong();
            }
        }
        throw new AssertionError("Expected numeric field among " + List.of(fields) + " in " + node);
    }

    private JsonNode firstExisting(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isMissingNode()) {
                return value;
            }
        }
        throw new AssertionError("Expected one field among " + List.of(fields) + " in " + node);
    }

    private record AnalyzeRequest(String pgn, PlayerColor playerColor) {
    }
}
