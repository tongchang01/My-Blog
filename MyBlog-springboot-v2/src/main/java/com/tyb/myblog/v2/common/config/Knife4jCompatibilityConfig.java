package com.tyb.myblog.v2.common.config;

import com.github.xiaoymin.knife4j.core.conf.GlobalConstants;
import com.github.xiaoymin.knife4j.spring.configuration.Knife4jProperties;
import com.github.xiaoymin.knife4j.spring.configuration.Knife4jSetting;
import com.github.xiaoymin.knife4j.spring.extension.Knife4jOpenApiCustomizer;
import com.github.xiaoymin.knife4j.spring.extension.OpenApiExtensionResolver;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Knife4j 4.5.0 与当前 springdoc 版本的兼容配置。
 *
 * <p>Knife4j 4.5.0 的默认 customizer 仍按旧签名调用
 * {@code SpringDocConfigProperties#getGroupConfigs()}，在 springdoc 2.8.x
 * 生成 OpenAPI 时会抛出 {@link NoSuchMethodError}。这里保留 Knife4j 的设置和
 * Markdown 扩展，只跳过不兼容的标签排序扫描。</p>
 */
@Configuration
@ConditionalOnProperty(
        name = "knife4j.enable",
        havingValue = "true")
public class Knife4jCompatibilityConfig {

    /**
     * 提供兼容当前 springdoc 的 Knife4j OpenAPI 扩展器。
     *
     * @param properties Knife4j 配置
     * @param springDocProperties springdoc 配置
     * @return 不调用旧 groupConfigs 方法的扩展器
     */
    @Bean
    Knife4jOpenApiCustomizer knife4jOpenApiCustomizer(
            Knife4jProperties properties,
            SpringDocConfigProperties springDocProperties
    ) {
        return new CompatibleKnife4jOpenApiCustomizer(
                properties,
                springDocProperties);
    }

    /**
     * 兼容 springdoc 2.8.x 的 Knife4j 扩展器。
     */
    private static final class CompatibleKnife4jOpenApiCustomizer
            extends Knife4jOpenApiCustomizer {

        private final Knife4jProperties properties;

        private CompatibleKnife4jOpenApiCustomizer(
                Knife4jProperties properties,
                SpringDocConfigProperties springDocProperties
        ) {
            super(properties, springDocProperties);
            this.properties = properties;
        }

        @Override
        public void customise(OpenAPI openApi) {
            if (!properties.isEnable()) {
                return;
            }
            Knife4jSetting setting = properties.getSetting();
            OpenApiExtensionResolver extensionResolver =
                    new OpenApiExtensionResolver(
                            setting,
                            properties.getDocuments());
            extensionResolver.start();

            Map<String, Object> extensions = new HashMap<>();
            extensions.put(
                    GlobalConstants.EXTENSION_OPEN_SETTING_NAME,
                    setting);
            extensions.put(
                    GlobalConstants.EXTENSION_OPEN_MARKDOWN_NAME,
                    extensionResolver.getMarkdownFiles());
            openApi.addExtension(
                    GlobalConstants.EXTENSION_OPEN_API_NAME,
                    extensions);
        }
    }
}
