package com.aurora.myblog.v2.modules.content.application;

import com.aurora.myblog.v2.modules.content.domain.CategorySummary;
import com.aurora.myblog.v2.modules.content.domain.ContentCatalogReader;
import com.aurora.myblog.v2.modules.content.domain.TagSummary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContentQueryService {

    private static final int DEFAULT_TOP_TAG_LIMIT = 10;
    private static final int MAX_TOP_TAG_LIMIT = 50;

    private final ContentCatalogReader catalogReader;

    public ContentQueryService(ContentCatalogReader catalogReader) {
        this.catalogReader = catalogReader;
    }

    public List<CategorySummary> listCategories() {
        return catalogReader.listCategories();
    }

    public List<TagSummary> listTags() {
        return catalogReader.listTags();
    }

    public List<TagSummary> listTopTags(Integer limit) {
        return catalogReader.listTopTags(normalizeTopTagLimit(limit));
    }

    private int normalizeTopTagLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_TOP_TAG_LIMIT;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_TOP_TAG_LIMIT);
    }
}
