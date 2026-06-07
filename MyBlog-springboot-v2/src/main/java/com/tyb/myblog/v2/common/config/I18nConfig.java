package com.tyb.myblog.v2.common.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * 服务端国际化配置。
 *
 * <p>后端 Locale 仅用于错误、校验和邮件等服务端消息。文章等业务内容的语言由接口路径决定。</p>
 */
@Configuration
public class I18nConfig {

    private static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;
    private static final List<Locale> SUPPORTED_LOCALES =
            List.of(DEFAULT_LOCALE, Locale.JAPANESE, Locale.ENGLISH);

    /**
     * 按 Accept-Language 解析服务端消息语言，不支持的语言回退中文。
     */
    @Bean
    LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(DEFAULT_LOCALE);
        resolver.setSupportedLocales(SUPPORTED_LOCALES);
        return resolver;
    }

    /**
     * 加载 UTF-8 编码的中、日、英服务端消息资源。
     */
    @Bean
    MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource =
                new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setDefaultLocale(DEFAULT_LOCALE);
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }
}
