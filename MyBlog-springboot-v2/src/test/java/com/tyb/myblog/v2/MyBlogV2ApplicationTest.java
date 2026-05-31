package com.tyb.myblog.v2;

import javax.sql.DataSource;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import io.swagger.v3.oas.models.OpenAPI;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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

    @Test
    void contextLoads() {
        assertThat(dataSource).isNotNull();
        assertThat(flyway).isNotNull();
        assertThat(mybatisPlusInterceptor).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("MyBlog 后端 V2 API");
    }
}
