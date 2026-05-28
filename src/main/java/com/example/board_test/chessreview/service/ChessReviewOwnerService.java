package com.example.board_test.chessreview.service;

import com.example.board_test.global.exception.CustomException;
import com.example.board_test.global.exception.ErrorCode;
import com.example.board_test.member.entity.Member;
import com.example.board_test.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChessReviewOwnerService {

    private final MemberRepository memberRepository;

    public Member resolve(String email) {
        if (email == null || email.isBlank()) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
