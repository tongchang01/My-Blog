package com.tyb.myblog.v2.content.infrastructure;

import com.tyb.myblog.v2.content.domain.CategorySummary;
import com.tyb.myblog.v2.content.domain.ContentCatalogReader;
import com.tyb.myblog.v2.content.domain.TagSummary;
import com.tyb.myblog.v2.content.infrastructure.persistence.mapper.ContentCatalogMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于旧库分类和标签表的内容目录读取器。
 *
 * <p>只统计未删除且已发布的文章，避免前台分类、标签数量包含草稿或软删除文章。</p>
 */
@Component
public class DatabaseContentCatalogReader implements ContentCatalogReader {

    private final ContentCatalogMapper contentCatalogMapper;

    public DatabaseContentCatalogReader(ContentCatalogMapper contentCatalogMapper) {
        this.contentCatalogMapper = contentCatalogMapper;
    }

    /**
     * 查询分类及已发布文章数量。
     */
    @Override
    public List<CategorySummary> listCategories() {
        return contentCatalogMapper.listCategorySummaries();
    }

    /**
     * 查询标签及已发布文章数量。
     */
    @Override
    public List<TagSummary> listTags() {
        return contentCatalogMapper.listTagSummaries();
    }

    /**
     * 查询按文章数量排序的热门标签。
     */
    @Override
    public List<TagSummary> listTopTags(int limit) {
        return contentCatalogMapper.listTopTagSummaries(Math.max(1, limit));
    }
}
