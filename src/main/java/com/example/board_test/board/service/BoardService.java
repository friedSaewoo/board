package com.example.board_test.board.service;

import com.example.board_test.board.dto.request.BoardCreateRequest;
import com.example.board_test.board.dto.response.BoardResponse;
import com.example.board_test.board.entity.Board;
import com.example.board_test.board.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;

    public BoardResponse create(BoardCreateRequest boardCreateRequest) {

        Board board = Board.from(
                boardCreateRequest.getTitle(),
                boardCreateRequest.getContents()
        );
        return BoardResponse.from(boardRepository.save(board));
    }

    public BoardResponse findById(Long id) {
        Board board = boardRepository.findById(id).orElse(null);
        return BoardResponse.from(board);
    }
}
