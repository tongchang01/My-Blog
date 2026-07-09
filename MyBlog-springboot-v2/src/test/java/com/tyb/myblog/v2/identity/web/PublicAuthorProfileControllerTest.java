package com.tyb.myblog.v2.identity.web;

import com.tyb.myblog.v2.common.error.GlobalExceptionHandler;
import com.tyb.myblog.v2.identity.application.profile.PublicAuthorProfileQueryService;
import com.tyb.myblog.v2.identity.application.profile.UserProfileResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicAuthorProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PublicAuthorProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublicAuthorProfileQueryService queryService;

    @Test
    void returnsPublicAuthorProfileWithoutInternalUserId() throws Exception {
        when(queryService.query()).thenReturn(new UserProfileResult(
                "TYB",
                "https://example.com/avatar.png",
                "中文简介",
                "日本語プロフィール",
                "English bio",
                "Tokyo",
                "https://example.com",
                "public@example.com",
                "https://github.com/tyb",
                "https://x.com/tyb",
                "https://linkedin.com/in/tyb",
                "https://zhihu.com/people/tyb",
                "https://qiita.com/tyb",
                "https://juejin.cn/user/tyb"));

        mockMvc.perform(get("/api/public/author-profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.nickname").value("TYB"))
                .andExpect(jsonPath("$.data.avatarUrl")
                        .value("https://example.com/avatar.png"))
                .andExpect(jsonPath("$.data.githubUrl")
                        .value("https://github.com/tyb"))
                .andExpect(jsonPath("$.data.twitterUrl")
                        .value("https://x.com/tyb"))
                .andExpect(jsonPath("$.data.zhihuUrl")
                        .value("https://zhihu.com/people/tyb"))
                .andExpect(jsonPath("$.data.juejinUrl")
                        .value("https://juejin.cn/user/tyb"))
                .andExpect(jsonPath("$.data.userId").doesNotExist());
    }
}
