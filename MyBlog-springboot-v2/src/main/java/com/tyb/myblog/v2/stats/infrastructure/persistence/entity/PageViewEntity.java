package com.tyb.myblog.v2.stats.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 页面访问明细实体。
 *
 * <p>该表是 append-only 高写入例外表，不继承通用审计基类。</p>
 */
@Getter
@Setter
@TableName("t_page_view")
public class PageViewEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    private String lang;

    private String visitorHash;

    private String referrer;

    private LocalDateTime createdAt;
}
