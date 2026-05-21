package com.example.board_test.board.service;

import com.example.board_test.board.dto.request.BoardCreateRequest;
import com.example.board_test.board.dto.response.BoardResponse;
import com.example.board_test.board.entity.Board;
import com.example.board_test.board.repository.BoardRepository;
import com.example.board_test.global.exception.CustomException;
import com.example.board_test.global.exception.ErrorCode;
import com.example.board_test.global.common.dto.page.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;

    @Transactional
    public BoardResponse create(BoardCreateRequest boardCreateRequest) {

        Board board = Board.from(
                boardCreateRequest.getTitle(),
                boardCreateRequest.getContents()
        );
        return BoardResponse.from(boardRepository.save(board));
    }


    public BoardResponse findById(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
        return BoardResponse.from(board);
    }

    @Transactional(readOnly = true)
    public PagedResult<BoardResponse> findAll(Pageable pageable) {
        Page<Board> boardPage = boardRepository.findAll(pageable);
        Page<BoardResponse> responsePage = boardPage.map(BoardResponse::from);
        return PagedResult.from(responsePage);
    }
}
