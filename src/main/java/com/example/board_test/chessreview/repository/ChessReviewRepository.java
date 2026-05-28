package com.example.board_test.chessreview.repository;

import com.example.board_test.chessreview.entity.ChessReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChessReviewRepository extends JpaRepository<ChessReview, Long> {

    Page<ChessReview> findAllByOwnerMemberId(Long ownerMemberId, Pageable pageable);

    Optional<ChessReview> findByIdAndOwnerMemberId(Long id, Long ownerMemberId);
}
