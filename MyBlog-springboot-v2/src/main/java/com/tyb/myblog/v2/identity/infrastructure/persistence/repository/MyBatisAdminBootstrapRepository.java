package com.tyb.myblog.v2.identity.infrastructure.persistence.repository;

import com.tyb.myblog.v2.identity.domain.account.AccountType;
import com.tyb.myblog.v2.identity.domain.account.UserAccount;
import com.tyb.myblog.v2.identity.domain.bootstrap.AdminBootstrapRepository;
import com.tyb.myblog.v2.identity.infrastructure.persistence.entity.UserAccountEntity;
import com.tyb.myblog.v2.identity.infrastructure.persistence.mapper.UserAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 基于 MyBatis-Plus 的首个管理员初始化仓储。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisAdminBootstrapRepository implements AdminBootstrapRepository {

    private final UserAccountMapper mapper;

    @Override
    public boolean existsActiveAdmin() {
        return mapper.existsActiveAdmin();
    }

    @Override
    public UserAccount createAdmin(String username, String passwordHash) {
        UserAccountEntity entity = new UserAccountEntity();
        entity.setUsername(username);
        entity.setPasswordHash(passwordHash);
        entity.setType(AccountType.ADMIN.databaseValue());
        entity.setTokenVersion(0);
        entity.setLoginFailCount(0);
        if (mapper.insert(entity) != 1) {
            throw new IllegalStateException("管理员账号创建失败");
        }
        return new UserAccount(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                AccountType.ADMIN,
                entity.getTokenVersion(),
                entity.getLoginFailCount(),
                entity.getLockedUntil());
    }
}
