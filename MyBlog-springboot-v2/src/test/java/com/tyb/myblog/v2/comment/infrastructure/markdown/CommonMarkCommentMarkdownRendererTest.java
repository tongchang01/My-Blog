package com.tyb.myblog.v2.comment.infrastructure.markdown;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommonMarkCommentMarkdownRendererTest {

    private final CommonMarkCommentMarkdownRenderer renderer =
            new CommonMarkCommentMarkdownRenderer();

    @Test
    void rendersMarkdownFormattingButRemovesUnsafeHtml() {
        String html = renderer.render("""
                支持 **粗体** 和 [链接](https://example.com)。
                <script>alert(1)</script>
                <a href="javascript:alert(1)">bad</a>
                ![x](https://example.com/x.png)
                """);

        assertThat(html).contains("<strong>粗体</strong>");
        assertThat(html).contains("https://example.com");
        assertThat(html).doesNotContain("script");
        assertThat(html).doesNotContain("javascript:");
        assertThat(html).doesNotContain("<img");
    }

    @Test
    void returnsEmptyParagraphFreeHtmlForBlankInput() {
        assertThat(renderer.render("   ")).isEmpty();
    }
}
