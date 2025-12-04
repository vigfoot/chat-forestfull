package com.forestfull.config;

import com.forestfull.member.MemberDTO;
import com.forestfull.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class TikTokController {

    private final TikTokProperties props;
    private final MemberService memberService;
    private final WebClient webClient = WebClient.create();

    /**
     * 틱톡에서 전달된 code를 받아 회원 처리
     * 기존 회원은 로그인, 신규 회원은 회원가입 후 로그인
     */
    @PostMapping("/callback")
    public Mono<ResponseEntity<MemberDTO.Member>> handleCallback(@RequestParam("code") String code) {
        return memberService.processTikTokOAuth(code)
                .map(ResponseEntity::ok)
                .onErrorResume(err -> Mono.just(ResponseEntity.badRequest().build()));
    }

    // 1) 클라이언트를 TikTok 로그인으로 리다이렉트
    @GetMapping("/auth/tiktok/login")
    public Mono<String> loginRedirect() {
        String url = "https://www.tiktok.com/v2/auth/authorize/?" +
                "client_key=" + props.getClientKey() +
                "&redirect_uri=" + props.getRedirectUri() +
                "&response_type=code" +
                "&scope=" + props.getScope() +
                "&state=some-random-state";  // 실제론 CSRF 방지 state 저장
        return Mono.just("redirect:" + url);
    }

    // 2) TikTok이 리다이렉트한 callback 처리
    @GetMapping("/auth/tiktok/callback")
    public Mono<String> callback(@RequestParam("code") String code,
                                 @RequestParam("state") String state) {
        // (state 검증 생략)
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_key", props.getClientKey());
        form.add("client_secret", props.getClientSecret());
        form.add("code", code);
        form.add("grant_type", "authorization_code");
        form.add("redirect_uri", props.getRedirectUri());

        return webClient.post()
                .uri("https://open.tiktokapis.com/v2/oauth/token/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(String.class)
                .map(body -> {
                    // body가 JSON — parse 해서 access_token, open_id, refresh_token 처리
                    System.out.println("TikTok OAuth token response: " + body);
                    return "redirect:/";  // 로그인 완료 후 리다이렉트
                });
    }
}