package com.tyb.myblog.v2;

import javax.sql.DataSource;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import io.swagger.v3.oas.models.OpenAPI;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.LocaleResolver;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class MyBlogV2ApplicationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Autowired
    private MybatisPlusInterceptor mybatisPlusInterceptor;

    @Autowired
    private OpenAPI openAPI;

    @Autowired
    private Clock clock;

    @Autowired
    private MetaObjectHandler metaObjectHandler;

    @Autowired
    private LocaleResolver localeResolver;

    @Autowired
    private MessageSource messageSource;

    @Test
    void contextLoads() {
        assertThat(dataSource).isNotNull();
        assertThat(flyway).isNotNull();
        assertThat(mybatisPlusInterceptor).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("MyBlog 后端 V2 API");
        assertThat(clock.getZone()).isEqualTo(ZoneId.of("Asia/Tokyo"));
        assertThat(metaObjectHandler).isNotNull();
        assertThat(localeResolver).isNotNull();
        assertThat(messageSource.getMessage("common.success", null, Locale.ENGLISH))
                .isEqualTo("Success");
    }
}
