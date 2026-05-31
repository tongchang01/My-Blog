package com.tyb.myblog.v2.content.domain;

import java.util.List;

/**
 * 内容目录读取端口。
 *
 * <p>封装分类和标签读取能力，供前台导航、筛选和热门标签展示使用。</p>
 */
public interface ContentCatalogReader {

    /**
     * 查询分类列表及文章数量。
     */
    List<CategorySummary> listCategories();

    /**
     * 查询标签列表及文章数量。
     */
    List<TagSummary> listTags();

    /**
     * 查询热门标签。
     */
    List<TagSummary> listTopTags(int limit);
}
