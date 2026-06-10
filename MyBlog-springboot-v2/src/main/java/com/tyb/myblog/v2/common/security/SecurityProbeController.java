package com.tyb.myblog.v2.common.security;

import com.tyb.myblog.v2.common.web.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 安全配置探针接口。
 *
 * <p>仅用于自动化测试验证公开接口和后台接口的鉴权边界。
 * 业务接口不应依赖这些探针返回值。</p>
 */
@Profile({"local", "test"})
@RestController
public class SecurityProbeController {

    /**
     * 公开探针，验证白名单接口可以匿名访问。
     */
    @GetMapping("/api/public/security-probe")
    ApiResponse<String> publicProbe() {
        return ApiResponse.ok("public");
    }

    /**
     * 与公开探针同路径的写探针，用于验证白名单必须同时匹配请求方法和路径。
     */
    @PostMapping("/api/public/security-probe")
    ApiResponse<String> publicWriteProbe() {
        return ApiResponse.ok("public-write");
    }

    /**
     * 后台探针，验证后台接口必须具备管理员权限。
     */
    @GetMapping("/api/admin/security-probe")
    ApiResponse<String> protectedProbe() {
        return ApiResponse.ok("protected");
    }
}
