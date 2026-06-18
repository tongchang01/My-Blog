package com.tyb.myblog.v2.stats.domain;

/**
 * 页面访问明细持久化端口。
 */
public interface PageViewRepository {

    long append(PageViewEvent event);
}
