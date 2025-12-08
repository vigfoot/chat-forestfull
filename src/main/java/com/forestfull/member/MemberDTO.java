package com.forestfull.member;

import com.forestfull.domain.User;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

public class MemberDTO {

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Member extends User {
        private String roles = "ROLE_USER";
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
