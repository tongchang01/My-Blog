package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.infrastructure.persistence.mapper.ArticleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArticleCommentCountService {

    private final ArticleMapper mapper;

    public void increment(long articleId, int delta) {
        if (delta == 0) {
            return;
        }
        if (mapper.incrementCommentCount(articleId, delta) != 1) {
            throw new ApiException(
                    ApiErrorCode.CONFLICT,
                    "文章评论数更新失败");
        }
    }
}
