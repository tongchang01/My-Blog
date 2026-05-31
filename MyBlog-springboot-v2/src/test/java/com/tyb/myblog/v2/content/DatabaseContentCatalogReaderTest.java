package com.tyb.myblog.v2.content;

import com.tyb.myblog.v2.content.infrastructure.DatabaseContentCatalogReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class DatabaseContentCatalogReaderTest {

    @Autowired
    private DatabaseContentCatalogReader reader;

    @Test
    void usesMyBatisPlusMapperAsPersistenceBoundary() {
        assertThat(DatabaseContentCatalogReader.class.getDeclaredFields())
                .noneMatch(field -> field.getType().equals(JdbcTemplate.class))
                .anyMatch(field -> field.getType().getName().endsWith(".ContentCatalogMapper"));
    }

    @Test
    void listsCategoriesWithPublishedArticleCountsOnly() {
        var categories = reader.listCategories();

        assertThat(categories).extracting("name").containsExactly("Java", "生活");
        assertThat(categories).extracting("articleCount").containsExactly(1L, 1L);
    }

    @Test
    void listsTagsWithPublishedArticleCountsOnly() {
        var tags = reader.listTags();

        assertThat(tags).extracting("name").containsExactly("Spring", "Vue", "重构");
        assertThat(tags).extracting("articleCount").containsExactly(1L, 1L, 2L);
    }

    @Test
    void listsTopTagsByPublishedArticleCountThenId() {
        var tags = reader.listTopTags(2);

        assertThat(tags).extracting("name").containsExactly("重构", "Spring");
        assertThat(tags).extracting("articleCount").containsExactly(2L, 1L);
    }
}
