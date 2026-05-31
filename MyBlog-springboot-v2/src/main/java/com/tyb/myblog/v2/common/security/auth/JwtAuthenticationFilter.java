package com.tyb.myblog.v2.common.security.auth;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器。
 *
 * <p>从 {@code Authorization: Bearer ...} 请求头中读取访问令牌，解析成功后写入
 * Spring Security 上下文。解析失败时不直接返回错误，由后续授权规则决定是否需要登录。</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * JWT 签发、解析和撤销服务。
     */
    private final JwtTokenService tokenService;

    /**
     * 创建 JWT 认证过滤器。
     *
     * @param tokenService token 服务
     */
    public JwtAuthenticationFilter(JwtTokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * 解析 Bearer token 并建立当前请求的认证上下文。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring("Bearer ".length());
            tokenService.parse(token).ifPresent(claims -> {
                AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                        claims.userId(),
                        claims.username(),
                        claims.roles());
                // Spring Security 角色权限使用 ROLE_ 前缀，JWT 中只保存业务角色名。
                var authorities = claims.roles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal, token, authorities));
            });
        }
        filterChain.doFilter(request, response);
    }
}
