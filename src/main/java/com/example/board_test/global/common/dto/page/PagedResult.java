package com.example.board_test.global.common.dto.page;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class PagedResult <T>{
    private final List<T> content;
    private final PageInfo pageInfo;

    private PagedResult(List<T> content, PageInfo pageInfo) {
        this.content = content;
        this.pageInfo = pageInfo;
    }

    public static <T> PagedResult<T> from(Page<T> page){
        return new PagedResult<>(page.getContent(), PageInfo.fromPage(page));
    }
}
