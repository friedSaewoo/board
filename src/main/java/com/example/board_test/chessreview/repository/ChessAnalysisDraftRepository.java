package com.example.board_test.chessreview.repository;

import com.example.board_test.chessreview.entity.ChessAnalysisDraft;
import com.example.board_test.chessreview.model.ChessAnalysisDraftStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChessAnalysisDraftRepository extends JpaRepository<ChessAnalysisDraft, Long> {

    Optional<ChessAnalysisDraft> findByAnalysisIdAndOwnerMemberId(String analysisId, Long ownerMemberId);

    List<ChessAnalysisDraft> findByStatusAndExpiresAtBefore(ChessAnalysisDraftStatus status, LocalDateTime expiresAt);
}
