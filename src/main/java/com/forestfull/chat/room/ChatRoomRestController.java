package com.forestfull.chat.room;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.forestfull.chat.ChatDTO;
import com.forestfull.chat.message.ChatMessageService;
import com.forestfull.common.token.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
public class ChatRoomRestController {

    private static final int RECENT_MESSAGE_LIMIT = 50;

    private final JwtUtil jwtUtil;
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    @PostMapping
    public ResponseEntity<ChatDTO.Room> createRoom(@RequestBody ChatDTO.Room roomInfo, HttpServletRequest request) {
        if (roomInfo.getName() == null) return ResponseEntity.badRequest().build();

        final Optional<DecodedJWT> jwtToken = jwtUtil.getJwtToken(request);
        if (jwtToken.isEmpty()) return ResponseEntity.badRequest().build();

        ChatDTO.Room created = chatRoomService.createRoom(roomInfo.getName(), Long.valueOf(jwtToken.get().getSubject()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<ChatDTO.Room>> getAllRooms() {
        return ResponseEntity.ok(chatRoomService.getAllRooms());
    }

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