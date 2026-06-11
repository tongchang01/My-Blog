package com.tyb.myblog.v2.identity.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserTokenVersionMapper {

    @Select("""
            select token_version
            from t_user_auth
            where id = #{userId}
              and deleted = 0
            """)
    Integer selectActiveTokenVersion(@Param("userId") long userId);
}
