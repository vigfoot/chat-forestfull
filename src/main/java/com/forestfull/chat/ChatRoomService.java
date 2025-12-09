package com.forestfull.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomMapper chatRoomMapper;

    public List<ChatDTO.Room> getAllRooms() {
        return chatRoomMapper.findAllRooms();
    }

    public ChatDTO.Room getRoomById(Long roomId) {
        return chatRoomMapper.findRoomById(roomId);
    }

    /** 방 생성 */
    @Transactional
    public ChatDTO.Room createRoom(String roomName, Long memberId) {
        ChatDTO.Room room = new ChatDTO.Room();
        room.setName(roomName);
        room.setCreatedBy(memberId);

        chatRoomMapper.createRoom(room);
        log.info("ROOM CREATED id={} name={} by {}", room.getId(), room.getName(), memberId);
        return room;
    }

    /** 방 삭제 */
    @Transactional
    public boolean deleteRoom(Long roomId) {
        int affected = chatRoomMapper.deleteRoom(roomId);
        return affected > 0;
    }

    public List<ChatDTO.Participant> getParticipants(Long roomId) {
        return chatRoomMapper.findParticipants(roomId);
    }

    /** 입장 처리 */
    @Transactional
    public boolean enterRoom(Long roomId, Long memberId) {
        boolean exists = chatRoomMapper.isUserInRoom(roomId, memberId);
        if (exists) {
            log.debug("User {} already in room {}", memberId, roomId);
            return false;
        }

        int res = chatRoomMapper.addParticipant(roomId, memberId);
        log.info("JOIN room={} member={} result={}", roomId, memberId, res);
        return res > 0;
    }

    /** 퇴장 처리 */
    @Transactional
    public boolean leaveRoom(Long roomId, Long memberId) {
        int res = chatRoomMapper.removeParticipant(roomId, memberId);
        return res > 0;
    }
}
