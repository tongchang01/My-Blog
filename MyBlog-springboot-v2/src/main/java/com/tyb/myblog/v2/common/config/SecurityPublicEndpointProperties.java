package com.tyb.myblog.v2.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 安全白名单配置。
 *
 * <p>绑定 {@code myblog.security}。白名单中的路径会跳过登录校验，生产环境必须谨慎配置，
 * 避免把后台管理接口或敏感写接口暴露为匿名访问。</p>
 *
 * @param publicEndpoints 允许匿名访问的接口路径表达式
 */
@ConfigurationProperties("myblog.security")
public record SecurityPublicEndpointProperties(List<String> publicEndpoints) {
}
