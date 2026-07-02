package com.tyb.myblog.v2.content.infrastructure.persistence.projection;

import com.tyb.myblog.v2.content.infrastructure.persistence.entity.TagEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TagArticleCountRow extends TagEntity {

    private Long articleCount;
}
