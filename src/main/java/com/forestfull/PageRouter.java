package com.forestfull;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class PageRouter {

    @GetMapping("/pages/admin/dashboard")
    String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/pages/admin/emoji")
    String adminEmoji() {
        return "admin/emoji";
    }

    @GetMapping("/pages/admin/users")
    String adminUsers() {
        return "admin/users";
    }

    @GetMapping("/pages/rooms")
    String chatRooms() {
        return "chat/rooms";
    }

    @GetMapping("/pages/rooms/{roomId}")
    String roomPage(@PathVariable Long roomId, Model model) {
        model.addAttribute("roomId", roomId);
        return "chat/room";
    }

    @GetMapping("/pages/signup")
    String signup(){
        return "signup";
    }
}