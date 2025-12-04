package com.forestfull.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tiktok")
public class TikTokProperties {
    private String clientKey;
    private String clientSecret;
    private String redirectUri;
    private String scope;
}