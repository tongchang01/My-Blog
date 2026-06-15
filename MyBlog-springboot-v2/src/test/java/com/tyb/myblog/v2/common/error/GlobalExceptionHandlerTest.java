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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                .andExpect(jsonPath("$.code").value("90001"))
                .andExpect(jsonPath("$.msg").value("title must not be blank"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    void keepsMalformedJsonAsBadRequestEnvelope() throws Exception {
        mockMvc.perform(post("/api/test/errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("90001"))
                .andExpect(jsonPath("$.msg").value("请求体格式错误"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void returnsValidationEnvelopeWhenRequiredQueryParameterIsMissing() throws Exception {
        mockMvc.perform(get("/api/test/errors/query-parameter"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("90001"))
                .andExpect(jsonPath("$.msg").value("缺少必填请求参数: lang"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void returnsBusinessEnvelope() throws Exception {
        mockMvc.perform(post("/api/test/errors/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("90004"))
                .andExpect(jsonPath("$.msg").value("标题重复"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void hidesInternalApiExceptionMessage() throws Exception {
        mockMvc.perform(post("/api/test/errors/internal"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("99999"))
                .andExpect(jsonPath("$.msg").value("系统内部错误"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @RestController
    static class ErrorProbeController {

        @PostMapping("/api/test/errors/validation")
        void validate(@Valid @RequestBody TitleRequest request) {
        }

        @GetMapping("/api/test/errors/query-parameter")
        void queryParameter(@RequestParam String lang) {
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
