package com.example.board_test.board.entity;

import com.example.board_test.board.dto.request.BoardRequest;
import com.example.board_test.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "boards")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Board extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String contents;

    public static Board from(String title, String contents) {
        return Board.builder()
                .title(title)
                .contents(contents)
                .build();
    }

    public void update(BoardRequest request) {
        if (request.getTitle() != null) {
            this.title = request.getTitle();
        }
        if (request.getContents() != null) {
            this.contents = request.getContents();
        }
    }

}
