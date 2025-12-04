package com.forestfull;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PageRouter {

    /*TODO: TEST*/
    @GetMapping("/verify")
    public String verifyPage() {
        return "verify-pages"; // templates/verify-pages.html
    }

    @GetMapping("/admin/emoji")
    String emojiPage() {
        return "admin-emoji";
    }

    @GetMapping("/admin/users")
    public String adminUsers() {
        return "admin-users"; // templates/admin-users.html (권한필터 적용)
    }
}