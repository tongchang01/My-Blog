package com.tyb.myblog.v2.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class I18nConfigTest {

    @Test
    void resolvesSupportedAcceptLanguageAndFallsBackToChinese() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(I18nConfig.class)) {
            LocaleResolver resolver = context.getBean(LocaleResolver.class);

            assertThat(resolve(resolver, "ja-JP,ja;q=0.9")).isEqualTo(Locale.JAPANESE);
            assertThat(resolve(resolver, "en-US,en;q=0.9")).isEqualTo(Locale.ENGLISH);
            assertThat(resolve(resolver, "zh-CN,zh;q=0.9")).isEqualTo(Locale.SIMPLIFIED_CHINESE);
            assertThat(resolve(resolver, "fr-FR,fr;q=0.9")).isEqualTo(Locale.SIMPLIFIED_CHINESE);
        }
    }

    @Test
    void loadsChineseJapaneseAndEnglishMessages() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(I18nConfig.class)) {
            MessageSource messageSource = context.getBean(MessageSource.class);

            assertThat(messageSource.getMessage("common.success", null, Locale.SIMPLIFIED_CHINESE))
                    .isEqualTo("成功");
            assertThat(messageSource.getMessage("common.success", null, Locale.JAPANESE))
                    .isEqualTo("成功");
            assertThat(messageSource.getMessage("common.success", null, Locale.ENGLISH))
                    .isEqualTo("Success");
        }
    }

    private Locale resolve(LocaleResolver resolver, String acceptLanguage) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", acceptLanguage);
        return resolver.resolveLocale(request);
    }
}
