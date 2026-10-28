package com.kieumai.notifier.business.model;

import java.util.List;
import java.util.function.Function;

public record ReportPage<T>(List<T> items, int page, int size, long totalElements, int totalPages) {
    public <R> ReportPage<R> map(Function<? super T, R> mapper) {
        return new ReportPage<>(
                items.stream().map(mapper).toList(),
                page,
                size,
                totalElements,
                totalPages
        );
    }
}
