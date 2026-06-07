package com.tyb.myblog.v2.common.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GlobalExceptionHandlerTest.ErrorProbeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.ErrorProbeController.class})
class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc;

    @Autowired
    GlobalExceptionHandlerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void returnsValidationEnvelope() throws Exception {
        mockMvc.perform(post("/api/test/errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("90001"))
                .andExpect(jsonPath("$.message").value("title must not be blank"));
    }

    @Test
    void keepsMalformedJsonAsBadRequestEnvelope() throws Exception {
        mockMvc.perform(post("/api/test/errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("90001"))
                .andExpect(jsonPath("$.message").value("请求体格式错误"));
    }

    @Test
    void returnsBusinessEnvelope() throws Exception {
        mockMvc.perform(post("/api/test/errors/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("90004"))
                .andExpect(jsonPath("$.message").value("标题重复"));
    }

    @Test
    void hidesInternalApiExceptionMessage() throws Exception {
        mockMvc.perform(post("/api/test/errors/internal"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("99999"))
                .andExpect(jsonPath("$.message").value("系统内部错误"));
    }

    @RestController
    static class ErrorProbeController {

        @PostMapping("/api/test/errors/validation")
        void validate(@Valid @RequestBody TitleRequest request) {
        }

        @PostMapping("/api/test/errors/business")
        void conflict() {
            throw new ApiException(ApiErrorCode.CONFLICT, "标题重复");
        }

        @PostMapping("/api/test/errors/internal")
        void internal() {
            throw new ApiException(ApiErrorCode.INTERNAL_ERROR, "database password leaked");
        }
    }

    record TitleRequest(@NotBlank(message = "title must not be blank") String title) {
    }
}
