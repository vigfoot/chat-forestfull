package com.forestfull.member;

import lombok.Data;

import java.time.LocalDateTime;

public class MemberDTO {

    @Data
    public static class Member {
        private Long id;
        private String tiktokUserId; // OAuth openId
        private String displayName;
        private String profileImage;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String createdBy;
        private String updatedBy;
    }

    @Data
    public static class Room {
        private Long roomId;
        private Long memberId;
        private LocalDateTime joinedAt;
    }
}
