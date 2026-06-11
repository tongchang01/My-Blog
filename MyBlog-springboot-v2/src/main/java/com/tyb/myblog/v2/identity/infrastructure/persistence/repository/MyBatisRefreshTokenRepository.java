package com.tyb.myblog.v2.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tyb.myblog.v2.identity.domain.token.RefreshTokenRecord;
import com.tyb.myblog.v2.identity.domain.token.RefreshTokenRepository;
import com.tyb.myblog.v2.identity.infrastructure.persistence.entity.RefreshTokenEntity;
import com.tyb.myblog.v2.identity.infrastructure.persistence.mapper.RefreshTokenMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 基于 MyBatis-Plus 的 refresh token 持久化适配器。
 *
 * <p>负责领域记录与数据库实体转换，不向上层暴露 Mapper 或持久化状态值。</p>
 */
@Repository
public class MyBatisRefreshTokenRepository implements RefreshTokenRepository {

    private final RefreshTokenMapper mapper;

    public MyBatisRefreshTokenRepository(RefreshTokenMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(RefreshTokenRecord token) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUserId(token.userId());
        entity.setTokenHash(token.tokenHash());
        entity.setExpiresAt(token.expiresAt());
        entity.setRevoked(token.revoked() ? 1 : 0);
        mapper.insert(entity);
    }

    @Override
    public Optional<RefreshTokenRecord> findActiveForUpdate(String tokenHash, LocalDateTime now) {
        return Optional.ofNullable(mapper.selectActiveForUpdate(tokenHash, now))
                .map(this::toDomain);
    }

    @Override
    public boolean revoke(long id) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setId(id);
        entity.setRevoked(1);
        return mapper.updateById(entity) == 1;
    }

    @Override
    public int revokeAllByUserId(long userId) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setRevoked(1);
        LambdaUpdateWrapper<RefreshTokenEntity> update = new LambdaUpdateWrapper<RefreshTokenEntity>()
                .eq(RefreshTokenEntity::getUserId, userId)
                .eq(RefreshTokenEntity::getRevoked, 0);
        return mapper.update(entity, update);
    }

    private RefreshTokenRecord toDomain(RefreshTokenEntity entity) {
        return new RefreshTokenRecord(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getRevoked() != null && entity.getRevoked() == 1);
    }
}
