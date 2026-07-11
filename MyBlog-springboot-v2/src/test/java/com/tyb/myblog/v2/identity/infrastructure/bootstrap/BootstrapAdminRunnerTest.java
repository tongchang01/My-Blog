package com.tyb.myblog.v2.identity.infrastructure.bootstrap;

import com.tyb.myblog.v2.common.config.BootstrapAdminProperties;
import com.tyb.myblog.v2.identity.application.bootstrap.BootstrapAdminApplicationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminRunnerTest {

    @Mock
    private BootstrapAdminApplicationService service;

    @Mock
    private BootstrapAdminProperties properties;

    @InjectMocks
    private BootstrapAdminRunner runner;

    @Test
    void delegatesBootstrapToApplicationService() throws Exception {
        runner.run(new DefaultApplicationArguments());

        verify(service).bootstrap(properties);
    }
}
