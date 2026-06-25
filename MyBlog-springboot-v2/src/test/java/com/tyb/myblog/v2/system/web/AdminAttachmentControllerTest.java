package com.tyb.myblog.v2.system.web;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.GlobalExceptionHandler;
import com.tyb.myblog.v2.common.storage.StorageType;
import com.tyb.myblog.v2.system.application.attachment.AttachmentPageResult;
import com.tyb.myblog.v2.system.application.attachment.AttachmentQueryService;
import com.tyb.myblog.v2.system.application.attachment.AttachmentResult;
import com.tyb.myblog.v2.system.application.attachment.AttachmentUploadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAttachmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminAttachmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttachmentUploadService uploadService;

    @MockitoBean
    private AttachmentQueryService queryService;

    private AuthenticatedPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new AuthenticatedPrincipal(
                "1001", "admin", List.of("ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadsMultipartFileAndReturnsPublicMetadata()
            throws Exception {
        when(uploadService.upload(eq(principal), any()))
                .thenReturn(result());
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png",
                new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/admin/attachments").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.id").value("10"))
                .andExpect(jsonPath("$.data.createdBy").value("1001"))
                .andExpect(jsonPath("$.data.publicUrl")
                        .value("http://localhost/media/a.png"))
                .andExpect(jsonPath("$.data.deleted").doesNotExist())
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist());

        verify(uploadService).upload(
                eq(principal),
                org.mockito.ArgumentMatchers.argThat(command ->
                        "cover.png".equals(command.originalFilename())
                                && command.inputStream() != null));
    }

    @Test
    void rejectsMissingMultipartFile() throws Exception {
        mockMvc.perform(multipart("/api/admin/attachments"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("90001"));
    }

    @Test
    void returnsPagedAttachmentsWithDefaultParameters() throws Exception {
        when(queryService.page(principal, 1, 20))
                .thenReturn(new AttachmentPageResult(
                        List.of(result()), 1, 1, 20));

        mockMvc.perform(get("/api/admin/attachments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value("10"))
                .andExpect(jsonPath("$.data.records[0].createdBy").value("1001"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.records[0].deleted")
                        .doesNotExist());
    }

    @Test
    void returnsAttachmentDetail() throws Exception {
        when(queryService.detail(principal, 10L))
                .thenReturn(result());

        mockMvc.perform(get("/api/admin/attachments/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("10"))
                .andExpect(jsonPath("$.data.createdBy").value("1001"))
                .andExpect(jsonPath("$.data.storageType").doesNotExist())
                .andExpect(jsonPath("$.data.bucket").doesNotExist())
                .andExpect(jsonPath("$.data.objectKey").doesNotExist())
                .andExpect(jsonPath("$.data.hashSha256").doesNotExist());
    }

    private AttachmentResult result() {
        return new AttachmentResult(
                10L, StorageType.LOCAL, "local",
                "attachments/a.png",
                "http://localhost/media/a.png",
                "image/png", 3L, 2, 3, "cover.png",
                "a".repeat(64),
                LocalDateTime.of(2026, 6, 14, 12, 0),
                1001L);
    }
}
