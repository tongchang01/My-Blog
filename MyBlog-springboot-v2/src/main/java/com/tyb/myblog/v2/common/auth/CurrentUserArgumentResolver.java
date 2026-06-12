package com.tyb.myblog.v2.common.auth;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * {@link CurrentUser} 参数解析器。
 *
 * <p>用于在 Controller 方法参数中直接注入当前登录用户，避免各接口重复读取
 * {@link SecurityContextHolder}。如果当前请求没有有效认证信息，会抛出统一的未登录异常。</p>
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver, WebMvcConfigurer {

    /**
     * 判断方法参数是否需要由当前解析器处理。
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().equals(AuthenticatedPrincipal.class);
    }

    /**
     * 从 Spring Security 上下文解析当前登录用户。
     */
    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal principal)) {
            throw new ApiException(ApiErrorCode.INVALID_TOKEN);
        }
        return principal;
    }

    /**
     * 将当前解析器注册到 Spring MVC。
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(this);
    }
}
