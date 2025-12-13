package com.forestfull.chat.room;

import com.forestfull.chat.ChatDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomMapper chatRoomMapper;
    private final ParticipantService participantService;

    /**
     * 모든 방 목록을 조회하고 실시간 인원수를 포함하여 반환합니다.
     * (클라이언트 요구사항에 따라 List<Participant>에 의존)
     */
    @Transactional(readOnly = true)
    public List<ChatDTO.Room> getAllRooms() {
        List<ChatDTO.Room> rooms = chatRoomMapper.findAllRooms();

        // 💡 참여자 목록을 DB에서 조회하여 설정 (성능 이슈 가능성 인지)
        return rooms.stream()
                .peek(room -> {
                    List<ChatDTO.Participant> participants = chatRoomMapper.findParticipants(room.getId());
                    room.setParticipantList(participants);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChatDTO.Room findRoomById(Long roomId) {
        ChatDTO.Room room = chatRoomMapper.findRoomById(roomId);
        if (room != null) {
            room.setParticipantList(chatRoomMapper.findParticipants(roomId));
        }
        return room;
    }

    /** 방 생성 (관리자 전용) */
    @Transactional
    public ChatDTO.Room createRoom(String roomName, Long memberId) {
        ChatDTO.Room room = new ChatDTO.Room();
        room.setName(roomName);
        room.setCreatedBy(memberId);

        chatRoomMapper.createRoom(roomName, memberId);
        log.info("ROOM CREATED id={} name={} by {}", room.getId(), room.getName(), memberId);

        // 🟢 ParticipantService에 새로운 방 초기화 위임
        participantService.initializeRoomCount(room.getId());
        room.setParticipantList(Collections.emptyList());

        return room;
    }

    /** 방 삭제 */
    @Transactional
    public boolean deleteRoom(Long roomId) {
        int affected = chatRoomMapper.deleteRoom(roomId);

        if (affected > 0) {
            // 🟢 ParticipantService에 카운터 제거 위임
            participantService.removeRoomCount(roomId);
        }
        return affected > 0;
    }

    @Transactional(readOnly = true)
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
        if (res > 0) {
            log.info("JOIN room={} member={} result={}", roomId, memberId, res);
            // 🟢 ParticipantService에 인원 증가 및 브로드캐스팅 위임
            participantService.incrementParticipantCount(roomId);
        }
        return res > 0;
    }

    /** 퇴장 처리 */
    @Transactional
    public boolean leaveRoom(Long roomId, Long memberId) {
        int res = chatRoomMapper.removeParticipant(roomId, memberId);
        if (res > 0) {
            // 🟢 ParticipantService에 인원 감소 및 브로드캐스팅 위임
            participantService.decrementParticipantCount(roomId);
        }
        return res > 0;
    }
}