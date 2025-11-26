package com.forestfull.chat;

import com.forestfull.member.MemberDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomMapper chatRoomMapper;
    private final ChatMessageMapper chatMessageMapper;

    // --- Room 관련 ---
    public void joinRoom(MemberDTO.Room room) {
        chatRoomMapper.joinRoom(room);
    }

    public void leaveRoom(Long roomId, Long memberId) {
        chatRoomMapper.leaveRoom(roomId, memberId);
    }

    // --- Message 관련 ---
    public void saveMessage(ChatMessageDTO.Message message) {
        chatMessageMapper.insertMessage(message);
    }

    public List<ChatMessageDTO.Message> getRecentMessages(Long roomId, int limit) {
        return chatMessageMapper.findRecentMessages(roomId, limit);
    }
}
