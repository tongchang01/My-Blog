package com.tyb.myblog.v2.common.infrastructure.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyb.myblog.v2.common.mail.MailSendCommand;
import com.tyb.myblog.v2.common.mail.MailSendResult;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLSession;

import static org.assertj.core.api.Assertions.assertThat;

class ResendMailSenderTest {

    @Test
    void disabledSenderDoesNotCallHttpClient() {
        CountingHttpClient httpClient = new CountingHttpClient(500, "error");
        ResendMailSender sender = new ResendMailSender(
                new ResendMailProperties(false, "", ""),
                httpClient,
                new ObjectMapper());

        MailSendResult result = sender.send(command());

        assertThat(result.success()).isTrue();
        assertThat(httpClient.calls).isZero();
    }

    @Test
    void nonSuccessResponseReturnsFailure() {
        CountingHttpClient httpClient = new CountingHttpClient(500, "error");
        ResendMailSender sender = new ResendMailSender(
                new ResendMailProperties(true, "key", "noreply@example.com"),
                httpClient,
                new ObjectMapper());

        MailSendResult result = sender.send(command());

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("500");
    }

    private static MailSendCommand command() {
        return new MailSendCommand(
                "to@example.com",
                "comment_reply",
                "subject",
                "body",
                Map.of());
    }

    private static final class CountingHttpClient extends HttpClient {
        private final int statusCode;
        private final String body;
        private int calls;

        private CountingHttpClient(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override
        public <T> HttpResponse<T> send(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler)
                throws IOException, InterruptedException {
            calls++;
            @SuppressWarnings("unchecked")
            T typedBody = (T) body;
            return new SimpleHttpResponse<>(statusCode, typedBody, request);
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return null;
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }
    }

    private record SimpleHttpResponse<T>(
            int statusCode,
            T body,
            HttpRequest request) implements HttpResponse<T> {

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (first, second) -> true);
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
