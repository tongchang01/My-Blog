package com.tyb.myblog.v2.comment.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tyb.myblog.v2.common.infrastructure.persistence.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("t_comment")
public class CommentEntity extends BaseEntity {

    private Integer targetType;

    private Long targetId;

    private Long parentId;

    private Long replyToCommentId;

    private Long replyToUserId;

    private String replyToNickname;

    private Long authorUserId;

    private String authorNickname;

    private String authorEmail;

    private String authorSite;

    private String authorIp;

    private String authorUserAgent;

    private String contentMd;

    private String contentHtml;

    private Integer auditStatus;
}
