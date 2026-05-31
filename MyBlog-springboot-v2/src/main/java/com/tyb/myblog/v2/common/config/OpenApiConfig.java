package com.tyb.myblog.v2.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc-openapi 基础配置。
 *
 * <p>该配置只定义 OpenAPI 文档元信息，不修改当前安全白名单。Swagger UI 和
 * OpenAPI JSON 是否对外开放，需要在后续部署阶段结合生产环境认证策略单独收口。</p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * 后端 V2 OpenAPI 文档定义。
     */
    @Bean
    OpenAPI myBlogV2OpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("MyBlog 后端 V2 API")
                        .description("MyBlog 后端 V2 单体接口文档，覆盖前台、后台与后续 Vue3 重构联调接口。")
                        .version("0.1.0"));
    }
}
