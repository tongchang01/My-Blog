package com.tyb.myblog.v2.system.web;

import com.tyb.myblog.v2.common.error.GlobalExceptionHandler;
import com.tyb.myblog.v2.system.application.friendlink.FriendLinkQueryService;
import com.tyb.myblog.v2.system.application.friendlink.PublicFriendLinkResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicFriendLinkController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PublicFriendLinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FriendLinkQueryService queryService;

    @Test
    void returnsOnlyPublicFriendLinkFields() throws Exception {
        when(queryService.publicList()).thenReturn(List.of(
                new PublicFriendLinkResult(
                        10L,
                        "Example",
                        "https://example.com",
                        "https://example.com/logo.png",
                        "介绍")));

        mockMvc.perform(get("/api/public/friend-links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].name")
                        .value("Example"))
                .andExpect(jsonPath("$.data[0].url")
                        .value("https://example.com"))
                .andExpect(jsonPath("$.data[0].avatarUrl")
                        .value("https://example.com/logo.png"))
                .andExpect(jsonPath("$.data[0].description")
                        .value("介绍"))
                .andExpect(jsonPath("$.data[0].status")
                        .doesNotExist())
                .andExpect(jsonPath("$.data[0].sortOrder")
                        .doesNotExist())
                .andExpect(jsonPath("$.data[0].createdAt")
                        .doesNotExist())
                .andExpect(jsonPath("$.data[0].updatedAt")
                        .doesNotExist())
                .andExpect(jsonPath("$.data[0].deleted")
                        .doesNotExist());
    }
}
