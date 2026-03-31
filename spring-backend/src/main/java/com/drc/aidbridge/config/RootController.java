package com.drc.aidbridge.config;

import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public ApiResponse<Map<String, String>> root() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("service", "AidBridge Spring Backend");
        info.put("status", "running");
        info.put("auth", "/api/auth/login");
        info.put("health", "/actuator/health");
        return ApiResponse.success("Server is running", info);
    }
}