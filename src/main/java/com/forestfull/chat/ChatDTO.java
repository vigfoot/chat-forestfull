package com.forestfull.chat;

import lombok.Data;

import java.time.LocalDateTime;

public class ChatDTO {

    @Data
    public static class Message {
        private Long id;
        private Long roomId;
        private Long memberId;
        private Long memberName;
        private String message;
        private String createdBy;
        private String updatedBy;
        private LocalDateTime sentAt;
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