package com.forestfull.config.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * com.forestfull.config.jwt
 *
 * @author vigfoot
 * @version 2025-11-27
 */
@Data
@Component
@ConfigurationProperties(prefix = "tiktok")
public class TikTokProperties {
    private String clientKey;
    private String clientSecret;
    private String redirectUri;
    private String scope;
}