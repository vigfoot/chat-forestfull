package com.forestfull.chat;

import com.forestfull.chat.message.ChatMessageService;
import com.forestfull.chat.room.ChatRoomService;
import com.forestfull.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    // 채팅 메시지 전송
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatDTO.Message message, Principal principal) {
        if (!(principal instanceof UsernamePasswordAuthenticationToken token)) return;

        Object userDetails = token.getPrincipal();
        if (!(userDetails instanceof User user)) return;

        message.setUser(user);
        message.setType(ChatDTO.Message.MessageType.TALK);

        ChatDTO.Message saved = chatMessageService.saveMessage(message);
        simpMessagingTemplate.convertAndSend("/topic/rooms/" + saved.getRoomId(), saved);
    }

    // 입장 이벤트
    @MessageMapping("/chat.enter")
    public void enterRoom(@Payload ChatDTO.Participant participant, Principal principal) {
        if (!(principal instanceof UsernamePasswordAuthenticationToken token)) return;

        Object userDetails = token.getPrincipal();
        if (!(userDetails instanceof User user)) return;

        chatRoomService.enterRoom(participant.getRoomId(), user.getId());

        ChatDTO.Message enterMsg = new ChatDTO.Message();
        enterMsg.setRoomId(participant.getRoomId());
        enterMsg.setType(ChatDTO.Message.MessageType.ENTER);
        enterMsg.setUser(user);
        enterMsg.setMessage(user.getDisplayName() + "님이 입장했습니다");

        simpMessagingTemplate.convertAndSend(
                "/topic/rooms/" + participant.getRoomId(),
                enterMsg
        );

        List<ChatDTO.Participant> updated = chatRoomService.getParticipants(participant.getRoomId());
        simpMessagingTemplate.convertAndSend(
                "/topic/rooms/" + participant.getRoomId() + "/participants",
                updated
        );
    }

    // 퇴장 이벤트
    @MessageMapping("/chat.leave")
    public void leaveRoom(@Payload ChatDTO.Participant participant, Principal principal) {
        if (!(principal instanceof UsernamePasswordAuthenticationToken token)) return;

        Object userDetails = token.getPrincipal();
        if (!(userDetails instanceof User user)) return;

        chatRoomService.leaveRoom(participant.getRoomId(), user.getId());

        ChatDTO.Message leaveMsg = new ChatDTO.Message();
        leaveMsg.setRoomId(participant.getRoomId());
        leaveMsg.setType(ChatDTO.Message.MessageType.LEAVE);
        leaveMsg.setUser(user);
        leaveMsg.setMessage(user.getDisplayName() + "님이 퇴장했습니다");

        simpMessagingTemplate.convertAndSend(
                "/topic/rooms/" + participant.getRoomId(),
                leaveMsg
        );


        List<ChatDTO.Participant> updated = chatRoomService.getParticipants(participant.getRoomId());
        simpMessagingTemplate.convertAndSend(
                "/topic/rooms/" + participant.getRoomId() + "/participants",
                updated
        );
    }
}