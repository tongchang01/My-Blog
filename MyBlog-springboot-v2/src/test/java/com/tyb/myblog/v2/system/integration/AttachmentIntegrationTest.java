package com.tyb.myblog.v2.system.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.security.auth.JwtTokenService;
import com.tyb.myblog.v2.system.application.attachment.AttachmentResult;
import com.tyb.myblog.v2.system.application.attachment.AttachmentUploadCommand;
import com.tyb.myblog.v2.system.application.attachment.AttachmentUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 附件上传、去重恢复和只读查询的完整集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties =
        "myblog.storage.local.root=${java.io.tmpdir}/myblog-v2-test/attachment-integration")
class AttachmentIntegrationTest {

    private static final Path STORAGE_ROOT = Path.of(
            System.getProperty("java.io.tmpdir"),
            "myblog-v2-test",
            "attachment-integration");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtTokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AttachmentUploadService uploadService;

    @BeforeEach
    void resetState() throws IOException {
        jdbcTemplate.update("delete from t_attachment");
        jdbcTemplate.update("delete from t_refresh_token");
        jdbcTemplate.update("delete from t_user_info");
        jdbcTemplate.update("delete from t_user_auth");
        deleteStorageRoot();
        Files.createDirectories(STORAGE_ROOT);
    }

    @Test
    void uploadsDeduplicatesRestoresAndAllowsDemoRead() throws Exception {
        String adminToken = token(1001L, "admin", 1, "ADMIN");
        String demoToken = token(1002L, "demo", 2, "DEMO");
        byte[] image = png();

        JsonNode first = upload(adminToken, image);
        JsonNode duplicate = upload(adminToken, image);
        long id = first.path("data").path("id").asLong();

        assertThat(duplicate.path("data").path("id").asLong())
                .isEqualTo(id);
        assertThat(attachmentCount()).isEqualTo(1);
        assertThat(storedFileCount()).isEqualTo(1);

        jdbcTemplate.update("""
                update t_attachment
                set deleted = 1, deleted_at = current_timestamp,
                    deleted_by = 1001
                where id = ?
                """, id);

        JsonNode restored = upload(adminToken, image);
        assertThat(restored.path("data").path("id").asLong())
                .isEqualTo(id);
        assertThat(jdbcTemplate.queryForObject(
                "select deleted from t_attachment where id = ?",
                Integer.class,
                id)).isZero();
        assertThat(storedFileCount()).isEqualTo(1);

        mockMvc.perform(get("/api/admin/attachments")
                        .header("Authorization", "Bearer " + demoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value(id))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20));
        mockMvc.perform(get("/api/admin/attachments/{id}", id)
                        .header("Authorization", "Bearer " + demoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));
    }

    @Test
    void returnsInternalErrorWhenDeduplicatedObjectIsMissing()
            throws Exception {
        String adminToken = token(1001L, "admin", 1, "ADMIN");
        byte[] image = png();
        upload(adminToken, image);
        deleteStorageRoot();

        mockMvc.perform(multipart("/api/admin/attachments")
                        .file(file(image))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("99999"));
        assertThat(attachmentCount()).isEqualTo(1);
    }

    @Test
    void rejectsInvalidImageAndUnauthorizedWrites() throws Exception {
        String adminToken = token(1001L, "admin", 1, "ADMIN");
        String demoToken = token(1002L, "demo", 2, "DEMO");
        MockMultipartFile text = new MockMultipartFile(
                "file",
                "fake.png",
                MediaType.IMAGE_PNG_VALUE,
                "not-an-image".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/admin/attachments")
                        .file(text)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("90001"));
        mockMvc.perform(multipart("/api/admin/attachments")
                        .file(file(png()))
                        .header("Authorization", "Bearer " + demoToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("10003"));
        mockMvc.perform(get("/api/admin/attachments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10002"));
    }

    @Test
    void concurrentDuplicateUploadsConvergeOnOneRecordAndObject()
            throws Exception {
        byte[] image = png();
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                "1001", "admin", List.of("ADMIN"));
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<AttachmentResult> first = executor.submit(
                    () -> uploadAfterSignal(start, principal, image));
            Future<AttachmentResult> second = executor.submit(
                    () -> uploadAfterSignal(start, principal, image));
            start.countDown();

            assertThat(first.get().id()).isEqualTo(second.get().id());
        } finally {
            executor.shutdownNow();
        }

        assertThat(attachmentCount()).isEqualTo(1);
        assertThat(storedFileCount()).isEqualTo(1);
    }

    private JsonNode upload(String token, byte[] content) throws Exception {
        String response = mockMvc.perform(multipart("/api/admin/attachments")
                        .file(file(content))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private MockMultipartFile file(byte[] content) {
        return new MockMultipartFile(
                "file",
                "cover.png",
                MediaType.IMAGE_PNG_VALUE,
                content);
    }

    private AttachmentResult uploadAfterSignal(
            CountDownLatch start,
            AuthenticatedPrincipal principal,
            byte[] content) throws Exception {
        start.await();
        return uploadService.upload(
                principal,
                new AttachmentUploadCommand(
                        "cover.png",
                        new ByteArrayInputStream(content)));
    }

    private byte[] png() throws IOException {
        BufferedImage image =
                new BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            assertThat(ImageIO.write(image, "png", output)).isTrue();
            return output.toByteArray();
        }
    }

    private String token(
            long id,
            String username,
            int type,
            String role) {
        jdbcTemplate.update("""
                insert into t_user_auth (
                    id, username, password_hash, type, token_version, deleted
                ) values (?, ?, ?, ?, 0, 0)
                """,
                id,
                username,
                passwordEncoder.encode("password"),
                type);
        jdbcTemplate.update("""
                insert into t_user_info (
                    user_id, nickname, deleted
                ) values (?, ?, 0)
                """,
                id,
                username);
        return tokenService.issueAccessToken(
                Long.toString(id),
                username,
                List.of(role),
                0).accessToken();
    }

    private int attachmentCount() {
        return jdbcTemplate.queryForObject(
                "select count(*) from t_attachment",
                Integer.class);
    }

    private long storedFileCount() throws IOException {
        if (!Files.exists(STORAGE_ROOT)) {
            return 0;
        }
        try (var files = Files.walk(STORAGE_ROOT)) {
            return files.filter(Files::isRegularFile).count();
        }
    }

    private void deleteStorageRoot() throws IOException {
        if (!Files.exists(STORAGE_ROOT)) {
            return;
        }
        try (var paths = Files.walk(STORAGE_ROOT)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
