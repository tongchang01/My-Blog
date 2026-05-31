package com.tyb.myblog.v2.common.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记 Controller 方法参数为当前登录用户。
 *
 * <p>只能用于 {@link AuthenticatedPrincipal} 类型参数，由 {@link CurrentUserArgumentResolver}
 * 从 Spring Security 上下文中解析。</p>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
