package com.tyb.myblog.v2.comment.infrastructure.markdown;

import com.tyb.myblog.v2.comment.domain.CommentMarkdownRenderer;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Component;

/**
 * 评论 Markdown 渲染器。评论来自匿名入口，输出必须先转义原始 HTML 再白名单清洗。
 */
@Component
public class CommonMarkCommentMarkdownRenderer implements CommentMarkdownRenderer {

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();
    private final PolicyFactory sanitizer =
            Sanitizers.FORMATTING.and(Sanitizers.LINKS);

    @Override
    public String render(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        String html = renderer.render(parser.parse(markdown.trim()));
        return sanitizer.sanitize(html).trim();
    }
}
