package com.tyb.myblog.v2.common.infrastructure.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyb.myblog.v2.common.mail.MailSender;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(ResendMailProperties.class)
public class MailConfiguration {

    @Bean
    HttpClient mailHttpClient() {
        return HttpClient.newHttpClient();
    }

    @Bean
    MailSender mailSender(
            ResendMailProperties properties,
            HttpClient mailHttpClient,
            ObjectMapper objectMapper) {
        if (!properties.enabled()) {
            return new NoopMailSender();
        }
        return new ResendMailSender(properties, mailHttpClient, objectMapper);
    }
}
