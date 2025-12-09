package com.forestfull.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
public class ChatRoomRestController {

    private final ChatRoomService chatRoomService;

    // 방 생성: body로 ChatDTO.Room (name, createdBy) 전달
    @PostMapping
    public ResponseEntity<ChatDTO.Room> createRoom(@RequestBody ChatDTO.Room request) {
        if (request.getName() == null || request.getCreatedBy() == null) {
            return ResponseEntity.badRequest().build();
        }
        ChatDTO.Room created = chatRoomService.createRoom(request.getName(), request.getCreatedBy());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // 전체 방 목록 조회
    @GetMapping
    public ResponseEntity<List<ChatDTO.Room>> getAllRooms() {
        return ResponseEntity.ok(chatRoomService.getAllRooms());
    }

    // 단일 방 조회
    @GetMapping("/{roomId}")
    public ResponseEntity<ChatDTO.Room> getRoom(@PathVariable Long roomId) {
        ChatDTO.Room room = chatRoomService.getRoomById(roomId);
        if (room == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(room);
    }

    @PostMapping("/{roomId}/enter")
    public ResponseEntity<Void> enterRoom(@PathVariable Long roomId, @RequestParam Long memberId) {
        boolean ok = chatRoomService.enterRoom(roomId, memberId);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }


    // 방 퇴장: memberId 쿼리 또는 바디로 제공 가능 (여기선 쿼리로 처리)
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable Long roomId, @RequestParam Long memberId) {
        boolean ok = chatRoomService.leaveRoom(roomId, memberId);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    // 방 삭제
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long roomId) {
        boolean ok = chatRoomService.deleteRoom(roomId);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // 참여자 목록
    @GetMapping("/{roomId}/participants")
    public ResponseEntity<List<ChatDTO.Participant>> getParticipants(@PathVariable Long roomId) {
        return ResponseEntity.ok(chatRoomService.getParticipants(roomId));
    }
}