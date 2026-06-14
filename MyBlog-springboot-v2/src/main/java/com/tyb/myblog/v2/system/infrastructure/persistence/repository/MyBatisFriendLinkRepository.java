package com.tyb.myblog.v2.system.infrastructure.persistence.repository;

import com.tyb.myblog.v2.system.domain.friendlink.FriendLink;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkPage;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkRepository;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkStatus;
import com.tyb.myblog.v2.system.domain.friendlink.NewFriendLink;
import com.tyb.myblog.v2.system.infrastructure.persistence.entity.FriendLinkEntity;
import com.tyb.myblog.v2.system.infrastructure.persistence.mapper.FriendLinkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 基于 MyBatis 的友链仓储适配器。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisFriendLinkRepository implements FriendLinkRepository {

    private final FriendLinkMapper mapper;

    @Override
    public List<FriendLink> findPublicVisible() {
        return mapper.selectPublicVisible().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public FriendLinkPage findActivePage(int page, int size) {
        long offset = Math.multiplyExact((long) page - 1L, size);
        return new FriendLinkPage(
                mapper.selectActivePage(offset, size).stream()
                        .map(this::toDomain)
                        .toList(),
                mapper.countActive(),
                page,
                size);
    }

    @Override
    public Optional<FriendLink> findActiveById(long id) {
        return Optional.ofNullable(mapper.selectActiveById(id))
                .map(this::toDomain);
    }

    @Override
    public Optional<FriendLink> findActiveByIdForUpdate(long id) {
        return Optional.ofNullable(
                        mapper.selectActiveByIdForUpdate(id))
                .map(this::toDomain);
    }

    @Override
    public List<FriendLink> findActiveByIdsForUpdate(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return mapper.selectActiveByIdsForUpdate(ids).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public FriendLink insert(NewFriendLink friendLink) {
        FriendLinkEntity entity = toEntity(friendLink);
        if (mapper.insert(entity) != 1
                || entity.getId() == null
                || entity.getId() <= 0) {
            throw new IllegalStateException("友链新增失败");
        }
        return toDomain(entity);
    }

    @Override
    public boolean update(
            FriendLink friendLink,
            LocalDateTime updatedAt,
            long updatedBy) {
        return mapper.updateActive(
                toEntity(friendLink), updatedAt, updatedBy) == 1;
    }

    @Override
    public boolean updateStatus(
            long id,
            FriendLinkStatus status,
            LocalDateTime updatedAt,
            long updatedBy) {
        return mapper.updateStatus(
                id, status.databaseValue(), updatedAt, updatedBy) == 1;
    }

    @Override
    public boolean updateSortOrder(
            long id,
            int sortOrder,
            LocalDateTime updatedAt,
            long updatedBy) {
        return mapper.updateSortOrder(
                id, sortOrder, updatedAt, updatedBy) == 1;
    }

    @Override
    public boolean softDelete(
            long id,
            LocalDateTime deletedAt,
            long deletedBy) {
        return mapper.softDelete(id, deletedAt, deletedBy) == 1;
    }

    private FriendLink toDomain(FriendLinkEntity entity) {
        return FriendLink.reconstitute(
                entity.getId(),
                entity.getName(),
                entity.getUrl(),
                entity.getAvatarUrl(),
                entity.getDescription(),
                entity.getSortOrder(),
                FriendLinkStatus.fromDatabaseValue(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy());
    }

    private FriendLinkEntity toEntity(NewFriendLink friendLink) {
        FriendLinkEntity entity = new FriendLinkEntity();
        entity.setName(friendLink.name());
        entity.setUrl(friendLink.url());
        entity.setAvatarUrl(friendLink.avatarUrl());
        entity.setDescription(friendLink.description());
        entity.setSortOrder(friendLink.sortOrder());
        entity.setStatus(friendLink.status().databaseValue());
        entity.setCreatedBy(friendLink.createdBy());
        entity.setUpdatedBy(friendLink.createdBy());
        return entity;
    }

    private FriendLinkEntity toEntity(FriendLink friendLink) {
        FriendLinkEntity entity = new FriendLinkEntity();
        entity.setId(friendLink.id());
        entity.setName(friendLink.name());
        entity.setUrl(friendLink.url());
        entity.setAvatarUrl(friendLink.avatarUrl());
        entity.setDescription(friendLink.description());
        entity.setSortOrder(friendLink.sortOrder());
        entity.setStatus(friendLink.status().databaseValue());
        return entity;
    }
}
