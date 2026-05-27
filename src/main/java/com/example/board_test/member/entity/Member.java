package com.example.board_test.member.entity;

import com.example.board_test.global.common.entity.BaseEntity;
import com.example.board_test.global.common.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Builder
@Table(name= "members")
@NoArgsConstructor
@AllArgsConstructor
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false,unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    public static Member from (String name,String email, String password){
        return Member.builder()
                .name(name)
                .email(email)
                .password(password)
                .role(UserRole.ROLE_USER)
                .build();
    }
}
