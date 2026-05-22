package com.example.board_test.global.common.dto.page;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Setter
public class PageQuery {

    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    private int pageNum = 1;

    @Min(value = 1,message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100,message = "페이지 크기는 100 이하여야 합니다.")
    private int pageSize = 10;

    private String sortBy = "id";

    private String direction = "DESC";

    public Pageable toPageable(){
        Sort.Direction dir = Sort.Direction.fromString(this.direction);
        return PageRequest.of(this.pageNum-1, this.pageSize, Sort.by(dir, this.sortBy));
    }
}
