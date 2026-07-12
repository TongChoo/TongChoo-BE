package com.asdf.tongchoobe.dto.response;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Spring Data의 Page<T>를 FE가 기대하는 페이지네이션 응답 형태로 옮겨 담는 래퍼. Backend.md §4.2.
 * Page<T> 자체를 그대로 직렬화하면 pageable/sort 등 FE에 불필요한 내부 구현 필드까지 노출되므로,
 * 목록 조회(예: GET /api/excuses) 응답은 항상 ApiResponse<PageResponse<T>>로 감싼다.
 */
@Getter
public class PageResponse<T> {
    private final List<T> content;
    private final int pageNumber;
    private final int pageSize;
    private final long totalElements;
    private final int totalPages;
    private final boolean first;
    private final boolean last;

    public PageResponse(Page<T> page) {
        this.content = page.getContent();
        this.pageNumber = page.getNumber();
        this.pageSize = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.first = page.isFirst();
        this.last = page.isLast();
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page);
    }
}
