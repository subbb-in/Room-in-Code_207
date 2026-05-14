package com.hamcheese.roomincode.global.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PageResponseDto<T> {
    private int currentPage;
    private int totalPage;
    private long totalElements;
    private List<T> content;
}
