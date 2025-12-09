package com.forestfull.chat;

import com.forestfull.domain.User;
import lombok.Data;
import java.time.LocalDateTime;

public class ChatDTO {

    /**
     * 🔹 채팅 메시지 DTO
     * WebSocket 실시간 처리 + DB 저장 겸용
     */
    @Data
    public static class Message {

        public enum MessageType {
            ENTER, TALK, LEAVE
        }

        private Long id;                  // DB 저장용
        private Long roomId;
        private User user;

        private MessageType type;         // 메시지 타입 (입장/퇴장/대화)
        private String message;           // 본문
        private LocalDateTime createAt;     // 송신 시간
        private String createdBy;         // DB 저장용
        private String updatedBy;
    }

    /**
     * 🔹 방 정보 DTO
     */
    @Data
    public static class Room {
        private Long id;
        private String name;
        private Long createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * 🔹 참여자 DTO
     */
    @Data
    public static class Participant {
        private Long memberId;
        private Long roomId;
        private String displayName;
        private String profileImage;
    }
}