package com.tyb.myblog.v2.content.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyb.myblog.v2.content.infrastructure.persistence.entity.ArticleAccessTokenEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface ArticleAccessTokenMapper extends BaseMapper<ArticleAccessTokenEntity> {

    int countActive(
            @Param("articleId") long articleId,
            @Param("tokenHash") String tokenHash,
            @Param("now") LocalDateTime now);

    int revokeActiveByArticleId(@Param("articleId") long articleId);
}
