package com.aurora.myblog.v2.common.web;

import java.util.List;

public record PageResponse<T>(List<T> records, long total, int page, int size) {

    public PageResponse {
        records = records == null ? List.of() : List.copyOf(records);
        if (total < 0) {
            throw new IllegalArgumentException("total must not be negative");
        }
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than zero");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be greater than zero");
        }
    }
}
