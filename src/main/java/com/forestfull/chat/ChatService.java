package com.forestfull.chat;

import com.forestfull.member.MemberDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final SimpMessagingTemplate template;
    private final ChatRoomMapper chatRoomMapper;
    private final ChatMessageMapper chatMessageMapper;

    public List<ChatDTO.Room> findAll() {
        return chatRoomMapper.findAll();
    }

    public ChatDTO.Room findById(Long roomId) {
        return chatRoomMapper.findById(roomId);
    }

    public ChatDTO.Room createRoom(String name, Long memberId) {
        ChatDTO.Room room = new ChatDTO.Room();
        room.setName(name);
        room.setCreatedBy(memberId);
        chatRoomMapper.insertRoom(room); // insert 후 room.id 채워짐 (useGeneratedKeys)
        return room;
    }

    @Transactional
    public void joinRoom(Long roomId, Long memberId) {
        if (chatRoomMapper.existsMemberInRoom(roomId, memberId)) return;
        chatRoomMapper.insertMemberRoom(roomId, memberId);
        broadcastParticipants(roomId);
    }

    @Transactional
    public void leaveRoom(Long roomId, Long memberId) {
        chatRoomMapper.deleteMemberRoom(roomId, memberId);
        broadcastParticipants(roomId);
    }

    public void saveMessage(ChatDTO.Message message) {
        chatMessageMapper.insertMessage(message);
    }

    public List<ChatDTO.Message> getRecentMessages(Long roomId, int limit) {
        return chatMessageMapper.findRecentMessages(roomId, limit);
    }

    public List<ChatDTO.Participant> getParticipants(Long roomId) {
        return chatRoomMapper.findParticipants(roomId);
    }

    public void broadcastParticipants(Long roomId) {
        List<ChatDTO.Participant> list = getParticipants(roomId);
        template.convertAndSend("/sub/chat/room/" + roomId + "/participants", list);
    }

    public void joinRoom(ChatDTO.Participant participant, StompHeaderAccessor accessor) {
        if (ObjectUtils.isEmpty(accessor.getSessionAttributes()))
            throw new RuntimeException();

        accessor.getSessionAttributes().put("roomId", participant.getRoomId());
        accessor.getSessionAttributes().put("memberId", participant.getMemberId());

        joinRoom(participant.getRoomId(), participant.getMemberId());
    }
}