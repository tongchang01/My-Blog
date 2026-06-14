package com.tyb.myblog.v2.common.security;

import com.tyb.myblog.v2.common.config.ApiCorsProperties;
import com.tyb.myblog.v2.common.config.SecurityJwtProperties;
import com.tyb.myblog.v2.common.config.SecurityPasswordProperties;
import com.tyb.myblog.v2.common.config.SecurityPublicEndpointProperties;
import com.tyb.myblog.v2.common.auth.BearerTokenResolver;
import com.tyb.myblog.v2.common.auth.token.AccessTokenVerifier;
import com.tyb.myblog.v2.common.security.auth.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * API 安全配置。
 *
 * <p>当前后端 V2 使用无状态 JWT 认证。白名单接口允许匿名访问，后台接口要求
 * {@code ADMIN} 角色，其余接口默认要求登录。生产环境需要配合安全的 JWT 密钥和明确的 CORS 来源。</p>
 */
@Configuration
@EnableConfigurationProperties({
        ApiCorsProperties.class,
        SecurityPublicEndpointProperties.class,
        SecurityJwtProperties.class,
        SecurityPasswordProperties.class
})
public class SecurityConfig {

    /**
     * 构建接口安全过滤链。
     *
     * <p>该配置关闭 session 和 HTTP Basic，避免前后端分离接口出现多套认证入口。</p>
     */
    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http,
                                    SecurityPublicEndpointProperties publicEndpointProperties,
                                    ObjectProvider<JwtAuthenticationFilter> jwtAuthenticationFilter,
                                    SecurityProblemSupport problemSupport) throws Exception {
        jwtAuthenticationFilter.ifAvailable(filter -> http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class));
        return http
                // 前后端分离接口使用 JWT，不依赖浏览器 Cookie，因此关闭 CSRF。
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                    publicEndpointProperties.publicEndpoints().forEach(endpoint ->
                            authorize.requestMatchers(endpoint.httpMethod(), endpoint.path()).permitAll());
                    authorize
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .requestMatchers(
                                    HttpMethod.PUT,
                                    "/api/auth/me/password")
                            .hasRole("ADMIN")
                            .requestMatchers(
                                    HttpMethod.PATCH,
                                    "/api/auth/me/profile")
                            .hasRole("ADMIN")
                            .requestMatchers(
                                    HttpMethod.GET,
                                    "/api/admin/site-config")
                            .hasAnyRole("ADMIN", "DEMO")
                            .requestMatchers(
                                    HttpMethod.GET,
                                    "/api/admin/attachments",
                                    "/api/admin/attachments/*")
                            .hasAnyRole("ADMIN", "DEMO")
                            .requestMatchers(
                                    HttpMethod.GET,
                                    "/api/admin/friend-links",
                                    "/api/admin/friend-links/*")
                            .hasAnyRole("ADMIN", "DEMO")
                            .requestMatchers("/api/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated();
                })
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, ex) -> problemSupport.writeUnauthorized(response))
                        .accessDeniedHandler((request, response, ex) -> problemSupport.writeForbidden(response)))
                .httpBasic(httpBasic -> httpBasic.disable())
                .build();
    }

    /**
     * JWT 认证过滤器。
     *
     * <p>只有 {@link AccessTokenVerifier} 存在时才注册，过滤器不绑定 JWT 或 identity 具体实现。</p>
     */
    @Bean
    @ConditionalOnBean(AccessTokenVerifier.class)
    JwtAuthenticationFilter jwtAuthenticationFilter(AccessTokenVerifier tokenVerifier,
                                                      BearerTokenResolver bearerTokenResolver) {
        return new JwtAuthenticationFilter(tokenVerifier, bearerTokenResolver);
    }

    /**
     * 安全异常响应写入器。
     */
    @Bean
    SecurityProblemSupport securityProblemSupport(ObjectMapper objectMapper) {
        return new SecurityProblemSupport(objectMapper);
    }

    /**
     * 密码加密器。
     *
     * <p>当前使用 BCrypt，适合保存后台账号密码摘要，禁止明文存储密码。</p>
     */
    @Bean
    PasswordEncoder passwordEncoder(SecurityPasswordProperties properties) {
        return new BCryptPasswordEncoder(properties.bcryptStrength());
    }

    /**
     * CORS 配置源。
     *
     * <p>允许携带凭证时，{@code allowedOrigins} 必须配置为明确来源，不能使用通配符。</p>
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(ApiCorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
