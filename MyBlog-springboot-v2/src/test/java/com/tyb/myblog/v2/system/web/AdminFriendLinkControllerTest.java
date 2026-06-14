package com.tyb.myblog.v2.system.web;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.GlobalExceptionHandler;
import com.tyb.myblog.v2.system.application.friendlink.FriendLinkPageResult;
import com.tyb.myblog.v2.system.application.friendlink.FriendLinkCreateService;
import com.tyb.myblog.v2.system.application.friendlink.FriendLinkQueryService;
import com.tyb.myblog.v2.system.application.friendlink.FriendLinkResult;
import com.tyb.myblog.v2.system.application.friendlink.FriendLinkUpdateService;
import com.tyb.myblog.v2.system.application.friendlink.FriendLinkStatusService;
import com.tyb.myblog.v2.system.application.friendlink.FriendLinkSortService;
import com.tyb.myblog.v2.system.application.friendlink.FriendLinkDeleteService;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminFriendLinkController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminFriendLinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FriendLinkQueryService queryService;

    @MockitoBean
    private FriendLinkCreateService createService;

    @MockitoBean
    private FriendLinkUpdateService updateService;

    @MockitoBean
    private FriendLinkStatusService statusService;

    @MockitoBean
    private FriendLinkSortService sortService;

    @MockitoBean
    private FriendLinkDeleteService deleteService;

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
    void returnsPagedLinksWithDefaultParameters() throws Exception {
        when(queryService.adminPage(principal, 1, 20))
                .thenReturn(new FriendLinkPageResult(
                        List.of(result()), 1, 1, 20));

        mockMvc.perform(get("/api/admin/friend-links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id")
                        .value(10))
                .andExpect(jsonPath("$.data.records[0].status")
                        .value("VISIBLE"))
                .andExpect(jsonPath("$.data.records[0].sortOrder")
                        .value(10))
                .andExpect(jsonPath("$.data.records[0].createdAt")
                        .value("2026-06-14T12:00:00"))
                .andExpect(jsonPath("$.data.records[0].updatedAt")
                        .value("2026-06-14T12:30:00"))
                .andExpect(jsonPath("$.data.records[0].deleted")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.records[0].deletedAt")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void returnsFriendLinkDetail() throws Exception {
        when(queryService.adminDetail(principal, 10L))
                .thenReturn(result());

        mockMvc.perform(get("/api/admin/friend-links/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.name")
                        .value("Example"))
                .andExpect(jsonPath("$.data.createdBy")
                        .value(1001))
                .andExpect(jsonPath("$.data.updatedBy")
                        .value(1001));
    }

    @Test
    void createsAndUpdatesCompleteFriendLink() throws Exception {
        when(createService.create(
                org.mockito.ArgumentMatchers.eq(principal),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(result());
        when(updateService.update(
                org.mockito.ArgumentMatchers.eq(principal),
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(result());

        mockMvc.perform(post("/api/admin/friend-links")
                        .contentType(
                                org.springframework.http.MediaType
                                        .APPLICATION_JSON)
                        .content(completeRequest()))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/friend-links/10")
                        .contentType(
                                org.springframework.http.MediaType
                                        .APPLICATION_JSON)
                        .content(completeRequest()))
                .andExpect(status().isOk());

        verify(createService).create(
                org.mockito.ArgumentMatchers.eq(principal),
                org.mockito.ArgumentMatchers.argThat(command ->
                        command.avatarUrl() == null
                                && command.description() == null));
        verify(updateService).update(
                org.mockito.ArgumentMatchers.eq(principal),
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.argThat(command ->
                        command.status()
                                == FriendLinkStatus.HIDDEN));
    }

    @Test
    void rejectsMissingUnknownAndInvalidEnumFields() throws Exception {
        mockMvc.perform(post("/api/admin/friend-links")
                        .contentType(
                                org.springframework.http.MediaType
                                        .APPLICATION_JSON)
                        .content(requestWithoutStatus()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("90001"));
        mockMvc.perform(put("/api/admin/friend-links/10")
                        .contentType(
                                org.springframework.http.MediaType
                                        .APPLICATION_JSON)
                        .content(completeRequest().replace(
                                "\"status\":\"HIDDEN\"",
                                "\"status\":\"HIDDEN\","
                                        + "\"unknown\":true")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("90001"));
        mockMvc.perform(post("/api/admin/friend-links")
                        .contentType(
                                org.springframework.http.MediaType
                                        .APPLICATION_JSON)
                        .content(completeRequest().replace(
                                "\"HIDDEN\"", "\"ARCHIVED\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("90001"));

        verifyNoInteractions(createService, updateService);
    }

    @Test
    void updatesStatusSortOrdersAndDeletes() throws Exception {
        when(statusService.update(
                org.mockito.ArgumentMatchers.eq(principal),
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(result());
        when(sortService.update(
                org.mockito.ArgumentMatchers.eq(principal),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(result()));

        mockMvc.perform(patch("/api/admin/friend-links/10/status")
                        .contentType(
                                org.springframework.http.MediaType
                                        .APPLICATION_JSON)
                        .content("{\"status\":\"HIDDEN\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/friend-links/sort-orders")
                        .contentType(
                                org.springframework.http.MediaType
                                        .APPLICATION_JSON)
                        .content("""
                                {"items":[{"id":10,"sortOrder":1}]}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/friend-links/10"))
                .andExpect(status().isOk());

        verify(deleteService).delete(principal, 10L);
    }

    private FriendLinkResult result() {
        return new FriendLinkResult(
                10L,
                "Example",
                "https://example.com",
                "https://example.com/logo.png",
                "介绍",
                10,
                FriendLinkStatus.VISIBLE,
                LocalDateTime.of(2026, 6, 14, 12, 0),
                1001L,
                LocalDateTime.of(2026, 6, 14, 12, 30),
                1001L);
    }

    private String completeRequest() {
        return """
                {
                  "name":"Example",
                  "url":"https://example.com",
                  "avatarUrl":null,
                  "description":null,
                  "sortOrder":10,
                  "status":"HIDDEN"
                }
                """;
    }

    private String requestWithoutStatus() {
        return """
                {
                  "name":"Example",
                  "url":"https://example.com",
                  "avatarUrl":null,
                  "description":null,
                  "sortOrder":10
                }
                """;
    }
}
