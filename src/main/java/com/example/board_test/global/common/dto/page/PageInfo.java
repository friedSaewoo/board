package com.example.board_test.global.common.dto.page;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
public class PageInfo {
    private final int pageNum;
    private final int pageSize;
    private final long totalElement;
    private final int totalPage;

    private final int blockStart;
    private final int blockEnd;
    private final boolean hasPrev;
    private final boolean hasNext;

    @Builder
    public PageInfo(int pageNum, int pageSize, long totalElement, int totalPage,
                    int blockStart, int blockEnd, boolean hasPrev, boolean hasNext) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.totalElement = totalElement;
        this.totalPage = totalPage;
        this.blockStart = blockStart;
        this.blockEnd = blockEnd;
        this.hasPrev = hasPrev;
        this.hasNext = hasNext;
    }

    public static <T> PageInfo fromPage(Page<T> page){
        int pageNum = page.getNumber()+1;
        int pageSize = page.getSize();
        long totalElement = page.getTotalElements();
        int totalPage = page.getTotalPages();

        int blockSize = 5;
        int blockStart = ((pageNum - 1) / blockSize) * blockSize + 1;
        int blockEnd = Math.min(blockStart + blockSize - 1, totalPage == 0 ? 1 : totalPage);

        boolean hasPrev = blockStart > 1;
        boolean hasNext = blockEnd < totalPage;

        return PageInfo.builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalElement(totalElement)
                .totalPage(totalPage)
                .blockStart(blockStart)
                .blockEnd(blockEnd)
                .hasPrev(hasPrev)
                .hasNext(hasNext)
                .build();
    }
}