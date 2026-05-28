package com.example.board_test.chessreview.service;

import com.example.board_test.global.exception.CustomException;
import com.example.board_test.global.exception.ErrorCode;
import com.example.board_test.member.entity.Member;
import com.example.board_test.member.repository.MemberRepository;
import org.springframework.stereotype.Service;

@Service
public class ChessReviewMemberService {

    private final MemberRepository memberRepository;

    public ChessReviewMemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member requireMember(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
