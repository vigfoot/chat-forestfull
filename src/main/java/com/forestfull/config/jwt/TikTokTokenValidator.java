package com.forestfull.config.jwt;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class TikTokTokenValidator {

    private final TikTokProperties props;
    private final WebClient webClient = WebClient.create();

    public TikTokTokenValidator(TikTokProperties props) {
        this.props = props;
    }

    public Mono<String> validate(String accessToken) {
        return webClient.get()
                .uri("https://open-api.tiktok.com/oauth/userinfo/?access_token={token}", accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> {
                    Map<String, Object> data = (Map<String, Object>) resp.get("data");
                    return (String) data.get("open_id"); // userId 역할
                });
    }
}