package com.forestfull.member;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberMapper memberMapper;
    private final WebClient webClient = WebClient.create();

    public Mono<MemberDTO.Member> processTikTokOAuth(String code) {
        return getAccessToken(code)
                .flatMap(token -> getUserInfo(token)
                        .map(user -> saveOrUpdateMember(user, token))
                );
    }

    private Mono<String> getAccessToken(String code) {
        return webClient.post()
                .uri("https://open-api.tiktok.com/oauth/access_token")
                .bodyValue("client_key=YOUR_CLIENT_KEY&client_secret=YOUR_CLIENT_SECRET&code=" + code + "&grant_type=authorization_code")
                .retrieve()
                .bodyToMono(String.class)
                .map(this::extractAccessToken);
    }

    private Mono<MemberDTO.Member> getUserInfo(String accessToken) {
        return webClient.get()
                .uri("https://open-api.tiktok.com/user/info/?access_token=" + accessToken)
                .retrieve()
                .bodyToMono(MemberDTO.Member.class);
    }

    private MemberDTO.Member saveOrUpdateMember(MemberDTO.Member user, String accessToken) {
        MemberDTO.Member existing = memberMapper.findByTikTokUserId(user.getTiktokUserId());
        if (existing != null) {
            existing.setDisplayName(user.getDisplayName());
            existing.setProfileImage(user.getProfileImage());
            memberMapper.saveMember(existing); // 필요시 update 쿼리로 교체
            return existing;
        } else {
            user.setTiktokUserId(accessToken); // WebSocket token용
            memberMapper.saveMember(user);
            return user;
        }
    }

    private String extractAccessToken(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            return root.path("data").path("access_token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse access token", e);
        }
    }
}