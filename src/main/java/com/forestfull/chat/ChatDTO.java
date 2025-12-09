package com.forestfull.chat;

import lombok.Data;
import java.time.LocalDateTime;

public class ChatDTO {

    @Data
    public static class Message {

        public enum MessageType {
            ENTER, TALK, LEAVE
        }

        private Long id;            // DB 저장용
        private Long roomId;
        private Long memberId;

        // 🔥 WebSocket 실시간 처리용 필드
        private MessageType type;
        private String sender;      // username(=memberName 대신)
        private String message;
        private LocalDateTime sentAt;

        private String createdBy;   // DB 저장용
        private String updatedBy;
    }

    @Data
    public static class Room {
        private Long id;
        private String name;
        private Long createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class Participant {
        private Long memberId;
        private Long roomId;
        private String displayName;
        private String profileImage;
    }
}