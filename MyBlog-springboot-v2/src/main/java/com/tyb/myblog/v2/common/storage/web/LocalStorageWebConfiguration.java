package com.tyb.myblog.v2.common.storage.web;

import com.tyb.myblog.v2.common.storage.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * LOCAL 附件的只读静态资源映射。
 */
@Configuration
@ConditionalOnBean(StorageProperties.class)
@RequiredArgsConstructor
public class LocalStorageWebConfiguration implements WebMvcConfigurer {

    private final StorageProperties properties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/**")
                .addResourceLocations(properties.getLocal().getRoot().toUri().toString())
                .setCacheControl(CacheControl.noCache());
    }
}
