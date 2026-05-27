package com.example.board_test.member.dto.response;

import com.example.board_test.global.common.enums.UserRole;
import com.example.board_test.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {
    private long id;
    private String name;
    private String email;
    private UserRole role;

    private MemberResponse(Member member){
        this.id = member.getId();
        this.name = member.getName();
        this.email = member.getEmail();
        this.role = member.getRole();
    }

    public static MemberResponse from(Member member){
        return new MemberResponse(member);
    }
}