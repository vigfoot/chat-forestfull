package com.forestfull.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 메시지 수신 처리
     * @param roomId 채팅방 ID
     * @param message 메시지 본문 DTO
     */
    @MessageMapping("/chat/{roomId}")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Payload ChatDTO.Message message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        // HandshakeInterceptor에서 저장한 username 가져오기
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        message.setSender(username);

        log.info("📩 [Message] room={}, from={}, message={}",
                roomId, username, message.getMessage());

        // 구독자에게 메시지 브로드캐스트
        messagingTemplate.convertAndSend("/sub/chat/" + roomId, message);
    }

    /**
     * 방 입장 알림
     */
    @MessageMapping("/chat/enter/{roomId}")
    public void enterRoom(
            @DestinationVariable Long roomId,
            @Payload ChatDTO.Message message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        message.setSender(username);
        message.setType(ChatDTO.Message.MessageType.ENTER);

        log.info("🚪 ENTER room={}, user={}", roomId, username);

        messagingTemplate.convertAndSend("/sub/chat/" + roomId, message);
    }

    /**
     * 방 퇴장 알림
     */
    @MessageMapping("/chat/leave/{roomId}")
    public void leaveRoom(
            @DestinationVariable Long roomId,
            @Payload ChatDTO.Message message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        message.setSender(username);
        message.setType(ChatDTO.Message.MessageType.LEAVE);

        log.info("🚶 LEAVE room={}, user={}", roomId, username);

        messagingTemplate.convertAndSend("/sub/chat/" + roomId, message);
    }
}