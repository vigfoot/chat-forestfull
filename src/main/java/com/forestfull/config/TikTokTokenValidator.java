package com.forestfull.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TikTokTokenValidator {

    private final WebClient webClient = WebClient.create();

    public Mono<String> validate(String accessToken) {
        return webClient.get()
                .uri("https://open-api.tiktok.com/oauth/userinfo/?access_token={token}", accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> {
                    Map<String, Object> data = (Map<String, Object>) resp.get("data");
                    return (String) data.get("open_id"); // WebSocket에서 userId 역할
                });
    }
}