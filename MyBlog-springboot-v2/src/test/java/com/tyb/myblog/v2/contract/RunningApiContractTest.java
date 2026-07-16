package com.tyb.myblog.v2.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyb.myblog.v2.identity.domain.account.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 通过随机端口上的真实 HTTP 服务器验证管理端关键请求契约。
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties =
        "myblog.storage.local.root=${java.io.tmpdir}/myblog-v2-test/running-api-contract")
class RunningApiContractTest {

    private static final String PASSWORD = "contract-password";

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.update("DELETE FROM t_article_tag");
        jdbcTemplate.update("DELETE FROM t_article");
        jdbcTemplate.update("DELETE FROM t_attachment");
        jdbcTemplate.update("DELETE FROM t_category");
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_info");
        jdbcTemplate.update("DELETE FROM t_user_auth");
        jdbcTemplate.update("DELETE FROM t_site_config");
        insertAccount(1001L, "admin", AccountType.ADMIN);
        insertAccount(1002L, "demo", AccountType.DEMO);
        jdbcTemplate.update("""
                INSERT INTO t_category (
                    id, name_zh, slug, sort_order,
                    created_at, created_by, updated_at, updated_by, deleted
                ) VALUES (10, '契约分类', 'contract-category', 0,
                    CURRENT_TIMESTAMP, 1001,
                    CURRENT_TIMESTAMP, 1001, 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO t_site_config (
                    id, site_title_zh, started_date, deleted
                ) VALUES (1, '初始标题', '2023-12-31', 0)
                """);
    }

    @Test
    void authenticatesRefreshesAndEnforcesDemoReadOnlyContract()
            throws Exception {
        TokenPair demo = login("demo");
        HttpResponse<String> readable = send(
                "GET", "/api/admin/site-config", null, demo.accessToken());
        assertSuccess(readable);
        assertThat(json(readable).at("/data/siteTitleZh").asText())
                .isEqualTo("初始标题");

        HttpResponse<String> forbidden = send(
                "POST", "/api/admin/articles", articlePayload("DEMO 拒绝"),
                demo.accessToken());
        assertThat(forbidden.statusCode()).isEqualTo(403);
        assertThat(json(forbidden).path("code").asText()).isEqualTo("10003");

        HttpResponse<String> refreshed = send(
                "POST", "/api/auth/refresh",
                Map.of("refreshToken", demo.refreshToken()), null);
        assertSuccess(refreshed);
        assertThat(json(refreshed).at("/data/accessToken").asText())
                .isNotBlank()
                .isNotEqualTo(demo.accessToken());
        assertThat(json(refreshed).at("/data/refreshToken").asText())
                .isNotBlank()
                .isNotEqualTo(demo.refreshToken());

        HttpResponse<String> replay = send(
                "POST", "/api/auth/refresh",
                Map.of("refreshToken", demo.refreshToken()), null);
        assertThat(replay.statusCode()).isEqualTo(401);
        assertThat(json(replay).path("code").asText()).isEqualTo("10002");
    }

    @Test
    void createsAndCompletelyUpdatesArticleOverHttp() throws Exception {
        String token = login("admin").accessToken();
        HttpResponse<String> created = send(
                "POST", "/api/admin/articles",
                articlePayload("契约草稿"), token);
        assertSuccess(created);
        String articleId = json(created).at("/data/id").asText();
        assertThat(articleId).isNotBlank();
        assertThat(json(created).at("/data/status").asText())
                .isEqualTo("DRAFT");

        Map<String, Object> update = articlePayload("契约已发布");
        update.put("summaryJa", "要約");
        update.put("status", "PUBLISHED");
        update.put("homepageSlot", "FEATURED");
        HttpResponse<String> updated = send(
                "PUT", "/api/admin/articles/" + articleId, update, token);
        assertSuccess(updated);
        JsonNode data = json(updated).path("data");
        assertThat(data.path("id").asText()).isEqualTo(articleId);
        assertThat(data.path("titleZh").asText()).isEqualTo("契约已发布");
        assertThat(data.path("summaryJa").asText()).isEqualTo("要約");
        assertThat(data.path("status").asText()).isEqualTo("PUBLISHED");
        assertThat(data.path("homepageSlot").asText()).isEqualTo("FEATURED");
        assertThat(data.path("body").asText()).isEqualTo("正文");
    }

    @Test
    void uploadsMultipartAttachmentOverHttp() throws Exception {
        String token = login("admin").accessToken();
        String boundary = "myblog-contract-boundary";
        byte[] body = multipart(boundary, png());
        HttpRequest request = HttpRequest.newBuilder(uri("/api/admin/attachments"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertSuccess(response);
        JsonNode data = json(response).path("data");
        assertThat(data.path("id").asText()).isNotBlank();
        assertThat(data.path("originalFilename").asText())
                .isEqualTo("contract.png");
        assertThat(data.path("contentType").asText()).isEqualTo("image/png");
        assertThat(data.path("publicUrl").asText())
                .startsWith("http://localhost/media/");
    }

    @Test
    void completelyUpdatesSiteConfigurationOverHttp() throws Exception {
        String token = login("admin").accessToken();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("siteTitleZh", "契约标题");
        payload.put("siteTitleJa", "契約タイトル");
        payload.put("siteTitleEn", "Contract title");
        payload.put("siteSubtitleZh", "副标题");
        payload.put("siteSubtitleJa", null);
        payload.put("siteSubtitleEn", "Subtitle");
        payload.put("aboutMdZh", "# 关于");
        payload.put("aboutMdJa", null);
        payload.put("aboutMdEn", "# About");
        payload.put("logoUrl", "https://example.com/logo.png");
        payload.put("faviconUrl", null);
        payload.put("icpNo", null);
        payload.put("spotifyPlaylistId", "playlist_123");
        payload.put("startedDate", "2024-01-02");

        HttpResponse<String> response = send(
                "PUT", "/api/admin/site-config", payload, token);
        assertSuccess(response);
        JsonNode data = json(response).path("data");
        assertThat(data.path("siteTitleZh").asText()).isEqualTo("契约标题");
        assertThat(data.path("siteTitleJa").asText()).isEqualTo("契約タイトル");
        assertThat(data.path("siteSubtitleJa").isNull()).isTrue();
        assertThat(data.path("startedDate").asText()).isEqualTo("2024-01-02");
    }

    private TokenPair login(String username) throws Exception {
        HttpResponse<String> response = send(
                "POST", "/api/auth/login",
                Map.of("username", username, "password", PASSWORD), null);
        assertSuccess(response);
        JsonNode data = json(response).path("data");
        return new TokenPair(
                data.path("accessToken").asText(),
                data.path("refreshToken").asText());
    }

    private Map<String, Object> articlePayload(String title) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("titleZh", title);
        payload.put("titleJa", null);
        payload.put("titleEn", null);
        payload.put("summaryZh", "摘要");
        payload.put("summaryJa", null);
        payload.put("summaryEn", null);
        payload.put("body", "正文");
        payload.put("categoryId", "10");
        payload.put("tagIds", List.of());
        payload.put("slug", null);
        payload.put("status", "DRAFT");
        payload.put("homepageSlot", "NONE");
        payload.put("password", null);
        payload.put("publishAt", null);
        payload.put("coverAttachmentId", null);
        return payload;
    }

    private HttpResponse<String> send(
            String method,
            String path,
            Object body,
            String accessToken) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .timeout(Duration.ofSeconds(10));
        if (accessToken != null) {
            request.header("Authorization", "Bearer " + accessToken);
        }
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(body),
                            StandardCharsets.UTF_8));
        }
        return httpClient.send(
                request.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private JsonNode json(HttpResponse<String> response) throws Exception {
        return objectMapper.readTree(response.body());
    }

    private void assertSuccess(HttpResponse<String> response) throws Exception {
        assertThat(response.statusCode())
                .withFailMessage(response.body())
                .isEqualTo(200);
        assertThat(json(response).path("code").asText()).isEqualTo("00000");
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private void insertAccount(long id, String username, AccountType type) {
        jdbcTemplate.update("""
                INSERT INTO t_user_auth (
                    id, username, password_hash, type,
                    token_version, deleted
                ) VALUES (?, ?, ?, ?, 0, 0)
                """,
                id,
                username,
                passwordEncoder.encode(PASSWORD),
                type.databaseValue());
    }

    private byte[] multipart(String boundary, byte[] file) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; "
                + "filename=\"contract.png\"\r\n"
                + "Content-Type: image/png\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        output.write(file);
        output.write(("\r\n--" + boundary + "--\r\n")
                .getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }

    private byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(
                2, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0x336699);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private record TokenPair(String accessToken, String refreshToken) {
    }
}
