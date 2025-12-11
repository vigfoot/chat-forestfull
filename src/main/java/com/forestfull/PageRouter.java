package com.forestfull;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class PageRouter {

    // Helper method to add the current URI to the model
    private void addCurrentUri(Model model, String uri) {
        model.addAttribute("currentUri", uri);
    }

    @GetMapping("/pages/admin/dashboard")
    String adminDashboard(Model model) {
        addCurrentUri(model, "/pages/admin/dashboard");
        return "admin/dashboard";
    }

    @GetMapping("/pages/admin/emoji")
    String adminEmoji(Model model) {
        addCurrentUri(model, "/pages/admin/emoji");
        return "admin/emoji";
    }

    @GetMapping("/pages/admin/users")
    String adminUsers(Model model) {
        addCurrentUri(model, "/pages/admin/users");
        return "admin/users";
    }

    @GetMapping("/pages/rooms")
    String chatRooms(Model model) {
        addCurrentUri(model, "/pages/rooms");
        return "chat/rooms";
    }

    @GetMapping("/pages/rooms/{roomId}")
    String roomPage(@PathVariable Long roomId, Model model) {
        model.addAttribute("roomId", roomId);
        addCurrentUri(model, "/pages/rooms/" + roomId); // Use the full URI for consistency
        return "chat/room";
    }

    @GetMapping("/pages/signup")
    String signup(Model model){
        // Sign up page typically doesn't need header context, but added for completeness
        addCurrentUri(model, "/pages/signup");
        return "account/signup";
    }

    @GetMapping("/pages/setting")
    String mySetting(Model model){
        // Sign up page typically doesn't need header context, but added for completeness
        addCurrentUri(model, "/pages/setting");
        return "account/setting";
    }
}