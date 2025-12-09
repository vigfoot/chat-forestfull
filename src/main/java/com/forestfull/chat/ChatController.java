package com.forestfull.chat;

import com.forestfull.chat.message.ChatMessageService;
import com.forestfull.chat.room.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/app")
public class ChatController {

    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    // 메시지 수신 → 저장 → 방에 브로드캐스트
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatDTO.Message message) {
        ChatDTO.Message saved = chatMessageService.saveMessage(message);
        simpMessagingTemplate.convertAndSend(
                "/topic/rooms/" + saved.getRoomId(),
                saved
        );
    }

    @MessageMapping("/chat.enter")
    public void enterRoom(@Payload ChatDTO.Participant participant, Principal principal) {
        chatRoomService.enterRoom(participant.getRoomId(), Long.valueOf(principal.getName()));

        List<ChatDTO.Participant> updated = chatRoomService.getParticipants(participant.getRoomId());
        simpMessagingTemplate.convertAndSend(
                "/topic/rooms/" + participant.getRoomId() + "/participants",
                updated
        );
    }

    @MessageMapping("/chat.leave")
    public void leaveRoom(@Payload ChatDTO.Participant participant, Principal principal) {
        chatRoomService.leaveRoom(participant.getRoomId(), Long.valueOf(principal.getName()));

        List<ChatDTO.Participant> updated = chatRoomService.getParticipants(participant.getRoomId());
        simpMessagingTemplate.convertAndSend(
                "/topic/rooms/" + participant.getRoomId() + "/participants",
                updated
        );
    }

}
