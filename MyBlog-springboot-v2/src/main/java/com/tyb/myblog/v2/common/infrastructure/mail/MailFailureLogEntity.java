package com.tyb.myblog.v2.common.infrastructure.mail;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("t_mail_log")
public class MailFailureLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String toEmail;

    private String template;

    private String subject;

    private Integer status;

    private Integer retryCount;

    private String errorMessage;

    private String providerMessageId;

    private String paramsJson;

    private LocalDateTime createdAt;
}
