package com.example.board_test.chessreview.repository;

import com.example.board_test.chessreview.entity.ChessAnalysisDraft;
import com.example.board_test.chessreview.model.ChessAnalysisDraftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ChessAnalysisDraftRepository extends JpaRepository<ChessAnalysisDraft, Long> {

    Optional<ChessAnalysisDraft> findByAnalysisIdAndOwnerMemberId(String analysisId, Long ownerMemberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ChessAnalysisDraft d set d.status = :expiredStatus where d.status = :activeStatus and d.expiresAt <= :now")
    int markExpiredDrafts(
            @Param("activeStatus") ChessAnalysisDraftStatus activeStatus,
            @Param("expiredStatus") ChessAnalysisDraftStatus expiredStatus,
            @Param("now") LocalDateTime now
    );
}
