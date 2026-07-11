package com.tyb.myblog.v2.identity.infrastructure.bootstrap;

import com.tyb.myblog.v2.common.config.BootstrapAdminProperties;
import com.tyb.myblog.v2.identity.application.bootstrap.BootstrapAdminApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 在显式启用时执行首个管理员初始化。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "myblog.bootstrap-admin",
        name = "enabled",
        havingValue = "true")
public class BootstrapAdminRunner implements ApplicationRunner {

    private final BootstrapAdminApplicationService service;
    private final BootstrapAdminProperties properties;

    @Override
    public void run(ApplicationArguments arguments) {
        service.bootstrap(properties);
    }
}
