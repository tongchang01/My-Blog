package com.tyb.myblog.v2.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Locale;

/**
 * 安全白名单配置。
 *
 * <p>绑定 {@code myblog.security}。白名单中的请求方法和路径会跳过登录校验，
 * 生产环境必须谨慎配置，避免把后台管理接口或敏感写接口暴露为匿名访问。</p>
 *
 * @param publicEndpoints 允许匿名访问的接口方法和路径表达式
 */
@ConfigurationProperties("myblog.security")
public record SecurityPublicEndpointProperties(List<PublicEndpoint> publicEndpoints) {

    /**
     * 允许匿名访问的接口定义。
     *
     * @param method HTTP 请求方法，例如 GET、POST
     * @param path   Spring Security 路径匹配表达式
     */
    public record PublicEndpoint(String method, String path) {

        /**
         * 将配置中的请求方法转换为 Spring Security 使用的 HTTP 方法枚举。
         */
        public HttpMethod httpMethod() {
            return HttpMethod.valueOf(method.toUpperCase(Locale.ROOT));
        }
    }
}
