package com.aurora.myblog.v2.modules.identity;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AdminIdentityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsMenusWithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/user/menus"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void rejectsMenusForNonAdminUser() throws Exception {
        String token = loginAndToken("user@163.com");

        mockMvc.perform(get("/api/admin/user/menus").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void returnsMenusForAdminUser() throws Exception {
        String token = loginAndToken("admin@163.com");

        mockMvc.perform(get("/api/admin/user/menus").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("首页"))
                .andExpect(jsonPath("$.data[1].name").value("文章管理"))
                .andExpect(jsonPath("$.data[1].children[0].name").value("文章列表"))
                .andExpect(jsonPath("$.data[1].children[1].name").value("草稿箱"))
                .andExpect(jsonPath("$.data[1].children[1].hidden").value(true));
    }

    private String loginAndToken(String username) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"password123"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.data.accessToken");
    }
}
