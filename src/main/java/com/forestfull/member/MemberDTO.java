package com.forestfull.member;

import lombok.Data;

import java.time.LocalDateTime;

public class MemberDTO {

    @Data
    public static class Member {
        private Long id;
        private String name; // OAuth openId
        private String password;
        private String displayName;
        private String profileImage;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String roles;
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
