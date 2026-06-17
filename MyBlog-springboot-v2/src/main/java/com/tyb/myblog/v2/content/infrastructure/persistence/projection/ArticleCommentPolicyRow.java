package com.tyb.myblog.v2.content.infrastructure.persistence.projection;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticleCommentPolicyRow {

    private Long id;

    private Integer status;

    private Integer commentCount;
}
