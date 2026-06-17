package com.tyb.myblog.v2.common.infrastructure.mail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyb.myblog.v2.common.mail.MailSendCommand;
import com.tyb.myblog.v2.common.mail.MailSendResult;
import com.tyb.myblog.v2.common.mail.MailSender;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class ResendMailSender implements MailSender {

    private static final URI RESEND_EMAIL_URI =
            URI.create("https://api.resend.com/emails");

    private final ResendMailProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ResendMailSender(
            ResendMailProperties properties,
            HttpClient httpClient,
            ObjectMapper objectMapper) {
        if (properties.enabled()
                && (isBlank(properties.apiKey())
                || isBlank(properties.fromEmail()))) {
            throw new IllegalStateException(
                    "Resend 已开启但 apiKey/fromEmail 未配置");
        }
        this.properties = properties;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public MailSendResult send(MailSendCommand command) {
        if (!properties.enabled()) {
            return MailSendResult.success(null);
        }
        try {
            HttpResponse<String> response = httpClient.send(
                    request(command),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return MailSendResult.success(providerMessageId(response.body()));
            }
            return MailSendResult.failed("Resend HTTP "
                    + response.statusCode()
                    + ": "
                    + response.body());
        } catch (IOException exception) {
            return MailSendResult.failed(exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return MailSendResult.failed(exception.getMessage());
        }
    }

    private HttpRequest request(MailSendCommand command)
            throws JsonProcessingException {
        Map<String, Object> body = Map.of(
                "from", properties.fromEmail(),
                "to", List.of(command.toEmail()),
                "subject", command.subject(),
                "text", command.bodyText());
        return HttpRequest.newBuilder(RESEND_EMAIL_URI)
                .header("Authorization", "Bearer " + properties.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(body)))
                .build();
    }

    private String providerMessageId(String responseBody)
            throws JsonProcessingException {
        return objectMapper.readTree(responseBody).path("id").asText(null);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
