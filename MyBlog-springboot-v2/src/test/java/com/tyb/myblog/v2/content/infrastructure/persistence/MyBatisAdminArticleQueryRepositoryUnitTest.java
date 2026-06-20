package com.tyb.myblog.v2.content.infrastructure.persistence;

import com.tyb.myblog.v2.content.domain.article.AdminArticleCriteria;
import com.tyb.myblog.v2.content.domain.article.AdminArticlePage;
import com.tyb.myblog.v2.content.infrastructure.persistence.mapper.ArticleMapper;
import com.tyb.myblog.v2.content.infrastructure.persistence.projection.AdminArticlePageRow;
import com.tyb.myblog.v2.content.infrastructure.persistence.projection.ArticleTagIdRow;
import com.tyb.myblog.v2.content.infrastructure.persistence.repository.MyBatisAdminArticleQueryRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MyBatisAdminArticleQueryRepositoryUnitTest {

    private final ArticleMapper mapper = mock(ArticleMapper.class);
    private final MyBatisAdminArticleQueryRepository repository =
            new MyBatisAdminArticleQueryRepository(mapper);

    @Test
    void loadsPageTagIdsWithOneBatchQuery() {
        AdminArticleCriteria criteria = new AdminArticleCriteria(
                1, 20, null, null, null, null,
                null, null, null, null);
        when(mapper.selectAdminPage(criteria, 0L, 20))
                .thenReturn(List.of(row(101L), row(100L)));
        when(mapper.selectTagIdsBatch(List.of(101L, 100L)))
                .thenReturn(List.of(
                        tag(100L, 20L),
                        tag(100L, 21L),
                        tag(101L, 22L)));
        when(mapper.countAdminPage(criteria)).thenReturn(2L);

        AdminArticlePage page = repository.findActivePage(criteria);

        assertThat(page.records())
                .extracting(item -> item.tagIds())
                .containsExactly(
                        List.of(22L),
                        List.of(20L, 21L));
        verify(mapper).selectTagIdsBatch(List.of(101L, 100L));
        verify(mapper, never()).selectTagIds(101L);
        verify(mapper, never()).selectTagIds(100L);
    }

    @Test
    void skipsTagQueryForEmptyPage() {
        AdminArticleCriteria criteria = new AdminArticleCriteria(
                1, 20, null, null, null, null,
                null, null, null, null);
        when(mapper.selectAdminPage(criteria, 0L, 20))
                .thenReturn(List.of());
        when(mapper.countAdminPage(criteria)).thenReturn(0L);

        AdminArticlePage page = repository.findActivePage(criteria);

        assertThat(page.records()).isEmpty();
        verify(mapper, never()).selectTagIdsBatch(anyList());
    }

    private static AdminArticlePageRow row(long id) {
        AdminArticlePageRow row = new AdminArticlePageRow();
        row.setId(id);
        row.setStatus(2);
        row.setCommentCount(0);
        return row;
    }

    private static ArticleTagIdRow tag(long articleId, long tagId) {
        ArticleTagIdRow row = new ArticleTagIdRow();
        row.setArticleId(articleId);
        row.setTagId(tagId);
        return row;
    }
}
