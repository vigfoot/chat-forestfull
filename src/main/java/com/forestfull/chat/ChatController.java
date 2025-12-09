package com.forestfull.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.util.List;

@Controller
@RequiredArgsConstructor
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

    // 유저 입장 이벤트
    @MessageMapping("/chat.enter")
    public void enterRoom(@Payload ChatDTO.Participant participant) {
        chatRoomService.enterRoom(participant.getRoomId(), participant.getMemberId());

        List<ChatDTO.Participant> updated = chatRoomService.getParticipants(participant.getRoomId());
        simpMessagingTemplate.convertAndSend(
                "/topic/rooms/" + participant.getRoomId() + "/participants",
                updated
        );
    }

    // 유저 퇴장 이벤트
    @MessageMapping("/chat.leave")
    public void leaveRoom(@Payload ChatDTO.Participant participant) {
        chatRoomService.leaveRoom(participant.getRoomId(), participant.getMemberId());

        List<ChatDTO.Participant> updated = chatRoomService.getParticipants(participant.getRoomId());
        simpMessagingTemplate.convertAndSend(
                "/topic/rooms/" + participant.getRoomId() + "/participants",
                updated
        );
    }
}
