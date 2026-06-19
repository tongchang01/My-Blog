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
 * @param publicEndpoints           所有环境共享的匿名业务端点
 * @param additionalPublicEndpoints 当前 Profile 追加的匿名端点
 */
@ConfigurationProperties("myblog.security")
public record SecurityPublicEndpointProperties(
        List<PublicEndpoint> publicEndpoints,
        List<PublicEndpoint> additionalPublicEndpoints) {

    public SecurityPublicEndpointProperties {
        publicEndpoints = immutable(publicEndpoints);
        additionalPublicEndpoints = immutable(additionalPublicEndpoints);
    }

    /**
     * 返回基础业务端点与当前 Profile 增量端点的有序并集。
     */
    public List<PublicEndpoint> allPublicEndpoints() {
        return java.util.stream.Stream.concat(
                        publicEndpoints.stream(),
                        additionalPublicEndpoints.stream())
                .toList();
    }

    private static List<PublicEndpoint> immutable(
            List<PublicEndpoint> endpoints) {
        return endpoints == null ? List.of() : List.copyOf(endpoints);
    }

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
