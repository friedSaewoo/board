package com.example.board_test.member.controller;

import com.example.board_test.member.dto.request.SignupRequest;
import com.example.board_test.member.dto.response.MemberResponse;
import com.example.board_test.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<MemberResponse> signup (@Valid @RequestBody SignupRequest request){
        return ResponseEntity.ok(memberService.signup(request));
    }

    @GetMapping("/me")
    public ResponseEntity<MemberResponse> me (Authentication authentication){
        return ResponseEntity.ok(memberService.me(authentication.getName()));
    }
}
