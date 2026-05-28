package com.aurora.myblog.v2.modules.content;

import com.aurora.myblog.v2.modules.content.domain.ArticlePageQuery;
import com.aurora.myblog.v2.modules.content.infrastructure.DatabaseArticleReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@JdbcTest
@Import(DatabaseArticleReader.class)
class DatabaseArticleReaderTest {

    @Autowired
    private DatabaseArticleReader reader;

    @Test
    void listsPublishedArticlesOnly() {
        var page = reader.listPublishedArticles(new ArticlePageQuery(1, 10));

        assertThat(page.total()).isEqualTo(2);
        assertThat(page.records()).extracting("id").containsExactly(2, 1);
        assertThat(page.records()).extracting("title").containsExactly("生活记录第一篇", "后端V2第一篇");
    }

    @Test
    void paginatesPublishedArticles() {
        var page = reader.listPublishedArticles(new ArticlePageQuery(1, 1));

        assertThat(page.total()).isEqualTo(2);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(1);
        assertThat(page.records()).extracting("id").containsExactly(2);
    }

    @Test
    void loadsArticleCardAssociationsWithoutDuplicatingArticle() {
        var page = reader.listPublishedArticles(new ArticlePageQuery(1, 10));

        var javaArticle = page.records().get(1);
        assertThat(javaArticle.category().name()).isEqualTo("Java");
        assertThat(javaArticle.author().id()).isEqualTo(1);
        assertThat(javaArticle.tags()).extracting("name").containsExactly("Spring", "重构");
        assertThat(javaArticle.top()).isTrue();
        assertThat(javaArticle.featured()).isTrue();
    }

    @Test
    void listsPublishedArticlesByCategory() {
        var page = reader.listPublishedArticlesByCategory(1, new ArticlePageQuery(1, 10));

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.records()).extracting("id").containsExactly(1);
    }

    @Test
    void listsPublishedArticlesByTag() {
        var page = reader.listPublishedArticlesByTag(3, new ArticlePageQuery(1, 10));

        assertThat(page.total()).isEqualTo(2);
        assertThat(page.records()).extracting("id").containsExactly(2, 1);
    }
}
