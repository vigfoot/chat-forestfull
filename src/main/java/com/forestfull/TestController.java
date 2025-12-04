package com.forestfull;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestController {

    @GetMapping("/api/secure")
    public Map<String, Object> secureEndpoint() {
        return Map.of("message", "protected data", "time", System.currentTimeMillis());
    }
}
