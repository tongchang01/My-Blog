package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.common.web.PageResponse;
import java.util.List;

public record PublicArticleHomeVO(
        PublicArticlePageItemVO pinnedArticle,
        List<PublicArticlePageItemVO> featuredArticles,
        PageResponse<PublicArticlePageItemVO> articles) {
}
