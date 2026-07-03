package com.tyb.myblog.v2.content.infrastructure.persistence.projection;

import com.tyb.myblog.v2.content.infrastructure.persistence.entity.CategoryEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryArticleCountRow extends CategoryEntity {

    private Long articleCount;
}
