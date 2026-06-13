package com.tyb.myblog.v2.identity.infrastructure.persistence.repository;

import com.tyb.myblog.v2.common.infrastructure.persistence.audit.SecurityContextAuditor;
import com.tyb.myblog.v2.identity.domain.profile.UserProfile;
import com.tyb.myblog.v2.identity.domain.profile.UserProfileRepository;
import com.tyb.myblog.v2.identity.infrastructure.persistence.entity.UserProfileEntity;
import com.tyb.myblog.v2.identity.infrastructure.persistence.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 基于 MyBatis-Plus 的用户资料仓储适配器。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisUserProfileRepository implements UserProfileRepository {

    private final UserProfileMapper mapper;
    private final Clock clock;
    private final SecurityContextAuditor auditor;

    @Override
    public Optional<UserProfile> findActiveByUserId(long userId) {
        return Optional.ofNullable(mapper.selectActiveByUserId(userId))
                .map(this::toDomain);
    }

    @Override
    public Optional<UserProfile> findActiveByUserIdForUpdate(long userId) {
        return Optional.ofNullable(mapper.selectActiveByUserIdForUpdate(userId))
                .map(this::toDomain);
    }

    @Override
    public void insert(UserProfile profile) {
        int affectedRows = mapper.insert(toEntity(profile));
        if (affectedRows != 1) {
            throw new IllegalStateException(
                    "用户资料创建失败，userId=" + profile.userId());
        }
    }

    @Override
    public boolean update(UserProfile profile) {
        return mapper.updateActiveProfile(
                toEntity(profile),
                LocalDateTime.now(clock),
                auditor.currentUserId()) == 1;
    }

    private UserProfile toDomain(UserProfileEntity entity) {
        return UserProfile.create(
                entity.getUserId(),
                entity.getNickname(),
                entity.getAvatarUrl(),
                entity.getBioZh(),
                entity.getBioJa(),
                entity.getBioEn(),
                entity.getLocation(),
                entity.getWebsite(),
                entity.getEmailPublic(),
                entity.getGithubUrl(),
                entity.getTwitterUrl(),
                entity.getLinkedinUrl(),
                entity.getZhihuUrl(),
                entity.getQiitaUrl(),
                entity.getJuejinUrl());
    }

    private UserProfileEntity toEntity(UserProfile profile) {
        UserProfileEntity entity = new UserProfileEntity();
        entity.setUserId(profile.userId());
        entity.setNickname(profile.nickname());
        entity.setAvatarUrl(profile.avatarUrl());
        entity.setBioZh(profile.bioZh());
        entity.setBioJa(profile.bioJa());
        entity.setBioEn(profile.bioEn());
        entity.setLocation(profile.location());
        entity.setWebsite(profile.website());
        entity.setEmailPublic(profile.emailPublic());
        entity.setGithubUrl(profile.githubUrl());
        entity.setTwitterUrl(profile.twitterUrl());
        entity.setLinkedinUrl(profile.linkedinUrl());
        entity.setZhihuUrl(profile.zhihuUrl());
        entity.setQiitaUrl(profile.qiitaUrl());
        entity.setJuejinUrl(profile.juejinUrl());
        return entity;
    }
}
