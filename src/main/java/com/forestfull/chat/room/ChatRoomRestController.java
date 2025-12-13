package com.forestfull.chat.room;

import com.forestfull.chat.ChatDTO;
import com.forestfull.chat.message.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
public class ChatRoomRestController {

    private static final int RECENT_MESSAGE_LIMIT = 50;

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final ParticipantService participantService;

    /**
     * GET /api/chat/rooms: 채팅방 목록 및 실시간 인원수 조회
     */
    @GetMapping
    public ResponseEntity<List<ChatDTO.Room>> getRooms() {

        // 1. 모든 ChatRoom 엔티티 조회 (DB 접근)
        List<ChatDTO.Room> rooms = chatRoomService.getAllRooms(); // ChatRoom 엔티티 반환 가정

        // 2. ChatDTO.Room DTO로 변환하면서 실시간 인원 정보 추가
        List<ChatDTO.Room> roomDtos = rooms.stream()
                .map(room -> {
                    // 💡 a. 실시간 인원수 조회
                    int count = participantService.getParticipantCount(room.getId());
                    // 💡 b. DTO로 변환 및 인원수 설정
                    return ChatDTO.Room.builder()
                            .id(room.getId())
                            .name(room.getName())
                            .createdBy(room.getCreatedBy()) // DTO의 필드 타입에 맞춰 설정
                            .createdAt(room.getCreatedAt())
                            .updatedAt(room.getUpdatedAt())
                            .participantsCount(count) // 인원수 설정
                            .build();
                })
                .toList();

        return ResponseEntity.ok(roomDtos);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<ChatDTO.Room> getRoom(@PathVariable Long roomId) {
        ChatDTO.Room room = chatRoomService.findRoomById(roomId);
        if (room == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(room);
    }

    @PostMapping("/{roomId}/enter")
    public ResponseEntity<Void> enterRoom(@PathVariable Long roomId, @RequestParam Long memberId) {
        boolean ok = chatRoomService.enterRoom(roomId, memberId);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable Long roomId, @RequestParam Long memberId) {
        boolean ok = chatRoomService.leaveRoom(roomId, memberId);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long roomId) {
        boolean ok = chatRoomService.deleteRoom(roomId);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/{roomId}/participants")
    public ResponseEntity<List<ChatDTO.Participant>> getParticipants(@PathVariable Long roomId) {
        return ResponseEntity.ok(chatRoomService.getParticipants(roomId));
    }

    // 🔥 추가: 최근 메시지 로드 (웹소켓 연결 직후 FE가 호출)
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<List<ChatDTO.Message>> getRecentMessages(@PathVariable Long roomId) {
        return ResponseEntity.ok(chatMessageService.getRecentMessages(roomId, RECENT_MESSAGE_LIMIT));
    }
}