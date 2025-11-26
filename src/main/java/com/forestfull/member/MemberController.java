package com.forestfull.member;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * com.forestfull.member
 *
 * @author vigfoot
 * @version 2025-11-25
 */
@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/oauth/callback/tiktok")
    public Mono<ResponseEntity<String>> tiktokCallback(@RequestParam("code") String code) {
        return memberService.processTikTokOAuth(code)
                .map(member -> ResponseEntity.ok("Welcome " + member.getDisplayName()))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }

}