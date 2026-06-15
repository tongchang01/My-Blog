package com.tyb.myblog.v2.content.domain;

import com.tyb.myblog.v2.content.domain.category.Category;
import com.tyb.myblog.v2.content.domain.category.NewCategory;
import com.tyb.myblog.v2.content.domain.tag.NewTag;
import com.tyb.myblog.v2.content.domain.tag.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryTagDomainTest {

    @Test
    void normalizesSlugAndFallsBackToChinese() {
        ContentSlug slug = ContentSlug.of("  Java-Spring  ");
        ContentName name = ContentName.of(" 后端 ", null, " ");

        assertThat(slug.value()).isEqualTo("java-spring");
        assertThat(name.localized(ContentLanguage.ZH)).isEqualTo("后端");
        assertThat(name.localized(ContentLanguage.JA)).isEqualTo("后端");
        assertThat(name.localized(ContentLanguage.EN)).isEqualTo("后端");
    }

    @Test
    void preservesOptionalLocalizedNamesAfterTrimming() {
        ContentName name = ContentName.of(
                " Java ",
                " ジャバ ",
                " Java language ");

        assertThat(name.zh()).isEqualTo("Java");
        assertThat(name.ja()).isEqualTo("ジャバ");
        assertThat(name.en()).isEqualTo("Java language");
    }

    @Test
    void parsesOnlySupportedLowercaseLanguages() {
        assertThat(ContentLanguage.parse("zh")).isEqualTo(ContentLanguage.ZH);
        assertThat(ContentLanguage.parse("ja")).isEqualTo(ContentLanguage.JA);
        assertThat(ContentLanguage.parse("en")).isEqualTo(ContentLanguage.EN);

        for (String value : new String[]{null, "", "ZH", "fr"}) {
            assertThatThrownBy(() -> ContentLanguage.parse(value))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "-java",
            "java-",
            "java--spring",
            "java_spring",
            "中文"
    })
    void rejectsInvalidSlug(String value) {
        assertThatThrownBy(() -> ContentSlug.of(value))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validatesNamesAndSlugLength() {
        assertThatThrownBy(() -> ContentName.of(" ", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ContentName.of("x".repeat(65), null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ContentName.of("中文", "x".repeat(65), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ContentSlug.of("a".repeat(65)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createsNormalizedCategoryAndTag() {
        NewCategory category = NewCategory.create(
                " 后端 ",
                null,
                " Backend ",
                " BACKEND ",
                10,
                1001L);
        NewTag tag = NewTag.create(
                " Java ",
                null,
                null,
                " JAVA ",
                1001L);

        assertThat(category.name().zh()).isEqualTo("后端");
        assertThat(category.name().en()).isEqualTo("Backend");
        assertThat(category.slug().value()).isEqualTo("backend");
        assertThat(category.sortOrder()).isEqualTo(10);
        assertThat(category.createdBy()).isEqualTo(1001L);
        assertThat(tag.name().zh()).isEqualTo("Java");
        assertThat(tag.slug().value()).isEqualTo("java");
        assertThat(tag.createdBy()).isEqualTo(1001L);
    }

    @Test
    void validatesCategorySortOrderAndCreator() {
        assertThatThrownBy(() -> NewCategory.create(
                "后端", null, null, "backend", -1, 1001L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NewCategory.create(
                "后端", null, null, "backend", 1_000_001, 1001L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NewCategory.create(
                "后端", null, null, "backend", 0, 0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NewTag.create(
                "Java", null, null, "java", -1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reconstitutesAndReplacesAggregatesWithoutChangingIdentityOrAudit() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 15, 11, 0);
        Category category = Category.reconstitute(
                101L,
                "后端",
                null,
                null,
                "backend",
                10,
                createdAt,
                1001L,
                updatedAt,
                1002L);
        Tag tag = Tag.reconstitute(
                201L,
                "Java",
                null,
                null,
                "java",
                createdAt,
                1001L,
                updatedAt,
                1002L);

        Category replacement = category.replace(
                "服务端",
                null,
                "Server",
                "SERVER",
                20);
        Tag tagReplacement = tag.replace(
                "Spring",
                null,
                null,
                "SPRING");

        assertThat(replacement.id()).isEqualTo(101L);
        assertThat(replacement.name().zh()).isEqualTo("服务端");
        assertThat(replacement.slug().value()).isEqualTo("server");
        assertThat(replacement.sortOrder()).isEqualTo(20);
        assertThat(replacement.createdAt()).isEqualTo(createdAt);
        assertThat(replacement.updatedAt()).isEqualTo(updatedAt);
        assertThat(tagReplacement.id()).isEqualTo(201L);
        assertThat(tagReplacement.name().zh()).isEqualTo("Spring");
        assertThat(tagReplacement.slug().value()).isEqualTo("spring");
        assertThat(tagReplacement.createdAt()).isEqualTo(createdAt);
    }
}
