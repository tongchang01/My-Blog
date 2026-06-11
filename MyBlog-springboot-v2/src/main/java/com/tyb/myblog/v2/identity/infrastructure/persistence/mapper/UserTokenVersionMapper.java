package com.tyb.myblog.v2.identity.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserTokenVersionMapper {

    @Select("""
            select token_version
            from t_user_auth
            where id = #{userId}
              and deleted = 0
            """)
    Integer selectActiveTokenVersion(@Param("userId") long userId);

    @Update("""
            update t_user_auth
            set token_version = token_version + 1,
                updated_at = current_timestamp,
                updated_by = #{userId}
            where id = #{userId}
              and deleted = 0
            """)
    int incrementActiveTokenVersion(@Param("userId") long userId);
}
