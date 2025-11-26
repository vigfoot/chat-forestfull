package com.forestfull.chat;

import lombok.Data;

import java.time.LocalDateTime;

public class ChatMessageDTO {

    @Data
    public static class Message {
        private Long id;
        private Long roomId;
        private Long memberId;
        private String message;
        private String createdBy;
        private String updatedBy;
        private LocalDateTime sentAt;
    }
}
