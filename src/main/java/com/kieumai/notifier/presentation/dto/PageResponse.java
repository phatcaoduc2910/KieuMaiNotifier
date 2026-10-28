package com.kieumai.notifier.presentation.dto;

import com.kieumai.notifier.business.model.ReportPage;
import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    public static <T> PageResponse<T> from(ReportPage<T> page) {
        return new PageResponse<>(page.items(), page.page(), page.size(),
                page.totalElements(), page.totalPages());
    }
}
