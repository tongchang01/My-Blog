package com.tyb.myblog.v2.system.application.siteconfig;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.system.domain.siteconfig.SiteConfig;
import com.tyb.myblog.v2.system.domain.siteconfig.SiteConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 后台站点配置查询服务测试。
 */
@ExtendWith(MockitoExtension.class)
class AdminSiteConfigQueryServiceTest {

    @Mock
    private SiteConfigRepository repository;

    private AdminSiteConfigQueryService service;

    @BeforeEach
    void setUp() {
        service = new AdminSiteConfigQueryService(repository);
    }

    @Test
    void permitsAdminAndDemoToReadCompleteConfiguration() {
        when(repository.findActive()).thenReturn(Optional.of(siteConfig()));

        AdminSiteConfigResult admin = service.query(principal("ADMIN"));
        AdminSiteConfigResult demo = service.query(principal("DEMO"));

        assertThat(admin.siteTitleZh()).isEqualTo("中文标题");
        assertThat(admin.siteTitleJa()).isEqualTo("日本語タイトル");
        assertThat(admin.updatedBy()).isEqualTo(1001L);
        assertThat(demo).isEqualTo(admin);
    }

    @Test
    void rejectsMissingPrincipal() {
        assertThatThrownBy(() -> service.query(null))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ApiErrorCode.INVALID_TOKEN));
    }

    @Test
    void rejectsInsufficientRole() {
        for (String role : List.of("GUEST", "UNKNOWN")) {
            assertThatThrownBy(() -> service.query(principal(role)))
                    .isInstanceOfSatisfying(
                            ApiException.class,
                            exception -> assertThat(exception.code())
                                    .isEqualTo(ApiErrorCode.FORBIDDEN));
        }
    }

    @Test
    void returnsInternalErrorWhenFixedConfigurationIsMissing() {
        when(repository.findActive()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.query(principal("ADMIN")))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ApiErrorCode.INTERNAL_ERROR));
    }

    private AuthenticatedPrincipal principal(String role) {
        return new AuthenticatedPrincipal("1001", "admin", List.of(role));
    }

    private SiteConfig siteConfig() {
        return SiteConfig.create(
                1L,
                "中文标题",
                "日本語タイトル",
                "English title",
                "中文副标题",
                "日本語サブタイトル",
                "English subtitle",
                "# 中文关于我",
                "# 私について",
                "# About",
                "https://example.com/logo.png",
                "https://example.com/favicon.ico",
                "京ICP备12345678号",
                "playlist_123",
                LocalDateTime.of(2026, 6, 14, 12, 0),
                1001L);
    }
}
