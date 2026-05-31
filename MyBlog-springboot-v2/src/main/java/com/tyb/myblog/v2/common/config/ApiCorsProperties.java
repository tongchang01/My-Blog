package com.tyb.myblog.v2.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * API 跨域配置。
 *
 * <p>绑定 {@code myblog.cors}。该配置控制浏览器允许哪些前端来源携带凭证访问后端接口。
 * 生产环境不应配置过宽的来源列表，避免后台接口被非预期站点调用。</p>
 *
 * @param allowedOrigins 允许跨域访问的前端来源列表
 */
@ConfigurationProperties("myblog.cors")
public record ApiCorsProperties(List<String> allowedOrigins) {
}
