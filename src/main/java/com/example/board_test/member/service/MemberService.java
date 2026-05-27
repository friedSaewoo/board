package com.example.board_test.member.service;

import com.example.board_test.global.exception.CustomException;
import com.example.board_test.global.exception.ErrorCode;
import com.example.board_test.member.dto.request.SignupRequest;
import com.example.board_test.member.dto.response.MemberResponse;
import com.example.board_test.member.entity.Member;
import com.example.board_test.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberResponse signup (SignupRequest request){

        if(memberRepository.existsByEmail(request.getEmail())){
           throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Member newMember = Member.from(request.getName(),request.getEmail(),passwordEncoder.encode(request.getPassword()));

        Member savedMember = memberRepository.save(newMember);
        return MemberResponse.from(savedMember);
    }

    public MemberResponse me (String email){
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        return MemberResponse.from(member);
    }
}
