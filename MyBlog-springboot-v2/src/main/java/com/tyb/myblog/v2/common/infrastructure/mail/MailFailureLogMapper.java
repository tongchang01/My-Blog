package com.tyb.myblog.v2.common.infrastructure.mail;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MailFailureLogMapper extends BaseMapper<MailFailureLogEntity> {

    int insertFailed(MailFailureLogEntity entity);
}
