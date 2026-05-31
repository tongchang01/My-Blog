package com.tyb.myblog.v2.common.security;

import com.tyb.myblog.v2.common.web.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurityProbeController {

    @GetMapping("/api/public/security-probe")
    ApiResponse<String> publicProbe() {
        return ApiResponse.ok("public");
    }

    @GetMapping("/api/admin/security-probe")
    ApiResponse<String> protectedProbe() {
        return ApiResponse.ok("protected");
    }
}
