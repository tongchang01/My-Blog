package com.tyb.myblog.v2.content.domain.article;

import java.util.Optional;

/**
 * 后台文章查询端口。
 */
public interface AdminArticleQueryRepository {

    AdminArticlePage findActivePage(AdminArticleCriteria criteria);

    Optional<AdminArticleDetail> findActiveDetail(long id);

    DeletedArticlePage findDeletedPage(int page, int size);
}
