package com.tyb.myblog.v2.common.web;

import com.tyb.myblog.v2.common.config.TrustedProxyProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 客户端 IP 解析工具。
 *
 * <p>仅当 Servlet 容器看到的远端地址属于可信代理时，才读取代理转发头；
 * 直连请求始终使用连接远端地址，避免客户端伪造转发头。</p>
 */
@Component
public class ClientIpResolver {

    private final List<IpAddressMatcher> trustedProxyMatchers;

    public ClientIpResolver(TrustedProxyProperties properties) {
        this.trustedProxyMatchers = properties.trustedProxies().stream()
                .map(IpAddressMatcher::new)
                .toList();
    }

    /**
     * 解析当前请求的客户端 IP。
     *
     * @param request 当前请求
     * @return 解析后的 IP；当所有来源都为空时返回 {@code null}
     */
    public String resolve(HttpServletRequest request) {
        String remoteAddress = normalize(request.getRemoteAddr());
        if (remoteAddress == null || !isTrustedProxy(remoteAddress)) {
            return remoteAddress;
        }

        String forwardedFor = firstForwardedIp(request.getHeader("X-Forwarded-For"));
        if (forwardedFor != null) {
            return forwardedFor;
        }
        String realIp = normalize(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }
        return remoteAddress;
    }

    private boolean isTrustedProxy(String remoteAddress) {
        return trustedProxyMatchers.stream().anyMatch(matcher -> matcher.matches(remoteAddress));
    }

    private static String firstForwardedIp(String value) {
        if (value == null) {
            return null;
        }
        // X-Forwarded-For 可能包含多级代理链，业务审计只记录最靠近客户端的第一个有效地址。
        for (String part : value.split(",")) {
            String ip = normalize(part);
            if (ip != null) {
                return ip;
            }
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        // 请求头来自外部环境，先裁剪空白，避免审计字段出现不可见差异。
        return value.trim();
    }
}
