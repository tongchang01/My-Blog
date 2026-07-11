package com.tyb.myblog.v2.identity.application.bootstrap;

import com.tyb.myblog.v2.common.config.BootstrapAdminProperties;
import com.tyb.myblog.v2.identity.domain.account.UserAccount;
import com.tyb.myblog.v2.identity.domain.auth.PasswordHashService;
import com.tyb.myblog.v2.identity.domain.bootstrap.AdminBootstrapRepository;
import com.tyb.myblog.v2.identity.domain.profile.UserProfile;
import com.tyb.myblog.v2.identity.domain.profile.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 创建首个管理员账号和资料。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BootstrapAdminApplicationService {

    private static final int MAX_USERNAME_LENGTH = 64;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;

    private final AdminBootstrapRepository adminBootstrapRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordHashService passwordHashService;

    /**
     * 若数据库尚无有效管理员，创建管理员账号和空资料。
     *
     * @param properties 初始化配置
     * @return 创建新管理员时返回 {@code true}，已有管理员时返回 {@code false}
     */
    @Transactional
    public boolean bootstrap(BootstrapAdminProperties properties) {
        String username = validateUsername(properties);
        String password = validatePassword(properties);
        if (adminBootstrapRepository.existsActiveAdmin()) {
            log.info("首个管理员初始化已跳过，当前已存在管理员，username={}", username);
            return false;
        }

        UserAccount account = adminBootstrapRepository.createAdmin(
                username,
                passwordHashService.encode(password));
        userProfileRepository.insert(UserProfile.create(
                account.id(),
                username,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));
        log.info("首个管理员初始化成功，username={}", username);
        return true;
    }

    private String validateUsername(BootstrapAdminProperties properties) {
        if (properties == null || properties.username() == null) {
            throw new IllegalArgumentException("管理员用户名不能为空");
        }
        String username = properties.username().trim();
        if (username.isEmpty() || username.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException("管理员用户名长度必须为1至64个字符");
        }
        return username;
    }

    private String validatePassword(BootstrapAdminProperties properties) {
        if (properties == null || properties.password() == null
                || properties.password().isBlank()
                || properties.password().length() < MIN_PASSWORD_LENGTH
                || properties.password().length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("管理员密码长度必须为8至128个字符");
        }
        return properties.password();
    }
}
