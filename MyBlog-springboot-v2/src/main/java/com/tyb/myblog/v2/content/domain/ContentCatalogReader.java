package com.tyb.myblog.v2.content.domain;

import java.util.List;

public interface ContentCatalogReader {

    List<CategorySummary> listCategories();

    List<TagSummary> listTags();

    List<TagSummary> listTopTags(int limit);
}
