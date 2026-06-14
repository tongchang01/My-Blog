package com.tyb.myblog.v2.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyb.myblog.v2.system.infrastructure.persistence.entity.FriendLinkEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 友链持久化 Mapper，SQL 统一位于 XML。
 */
@Mapper
public interface FriendLinkMapper extends BaseMapper<FriendLinkEntity> {

    List<FriendLinkEntity> selectPublicVisible();

    List<FriendLinkEntity> selectActivePage(
            @Param("offset") long offset,
            @Param("size") int size);

    long countActive();

    FriendLinkEntity selectActiveById(@Param("id") long id);

    FriendLinkEntity selectActiveByIdForUpdate(@Param("id") long id);

    List<FriendLinkEntity> selectActiveByIdsForUpdate(
            @Param("ids") List<Long> ids);

    int updateActive(
            @Param("link") FriendLinkEntity link,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") long updatedBy);

    int updateStatus(
            @Param("id") long id,
            @Param("status") int status,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") long updatedBy);

    int updateSortOrder(
            @Param("id") long id,
            @Param("sortOrder") int sortOrder,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") long updatedBy);

    int softDelete(
            @Param("id") long id,
            @Param("deletedAt") LocalDateTime deletedAt,
            @Param("deletedBy") long deletedBy);
}
