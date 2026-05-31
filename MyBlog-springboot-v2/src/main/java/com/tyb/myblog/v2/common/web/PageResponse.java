package com.tyb.myblog.v2.common.web;

import java.util.List;

/**
 * 统一分页响应。
 *
 * <p>用于前台列表、后台管理列表等分页接口。构造时会复制记录集合，避免调用方在响应创建后继续修改数据。</p>
 *
 * @param records 当前页记录
 * @param total   符合查询条件的总记录数，不能为负数
 * @param page    当前页码，从 1 开始
 * @param size    每页大小，必须大于 0
 */
public record PageResponse<T>(List<T> records, long total, int page, int size) {

    /**
     * 校验分页响应的基础边界。
     */
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
