package com.tyb.myblog.v2.identity.infrastructure.config;

import com.tyb.myblog.v2.common.config.SecurityPasswordProperties;
import com.tyb.myblog.v2.identity.domain.account.UserAccountRepository;
import com.tyb.myblog.v2.identity.domain.auth.LoginCredentialVerifier;
import com.tyb.myblog.v2.identity.domain.auth.LoginStateRecorder;
import com.tyb.myblog.v2.identity.domain.auth.LoginLockPolicy;
import com.tyb.myblog.v2.identity.domain.auth.PasswordHashVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 后台登录领域服务装配配置。
 */
@Configuration
public class IdentityLoginConfiguration {

    /**
     * 根据外部安全配置创建登录锁定策略。
     */
    @Bean
    LoginLockPolicy loginLockPolicy(SecurityPasswordProperties properties) {
        return new LoginLockPolicy(
                properties.loginMaxAttempts(),
                properties.loginCooldown()
        );
    }

    /**
     * 创建后台登录凭据校验器。
     */
    @Bean
    LoginCredentialVerifier loginCredentialVerifier(
            UserAccountRepository repository,
            PasswordHashVerifier passwordHashVerifier,
            LoginStateRecorder loginStateRecorder,
            LoginLockPolicy loginLockPolicy
    ) {
        return new LoginCredentialVerifier(
                repository,
                passwordHashVerifier,
                loginStateRecorder,
                loginLockPolicy
        );
    }
}
