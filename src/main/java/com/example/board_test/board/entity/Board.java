package com.example.board_test.board.entity;

import com.example.board_test.board.dto.request.BoardCreateRequest;
import com.example.board_test.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "board")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Board extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String contents;

    public static Board from(String title, String contents) {
        return Board.builder()
                .title(title)
                .contents(contents)
                .build();
    }
}
