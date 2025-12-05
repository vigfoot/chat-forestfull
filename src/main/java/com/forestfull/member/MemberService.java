package com.forestfull.member;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberMapper memberMapper;

    private MemberDTO.Member saveOrUpdateMember(MemberDTO.Member user, String accessToken) {
        MemberDTO.Member existing = memberMapper.findByTikTokUserId(user.getTiktokUserId());
        if (existing != null) {
            existing.setDisplayName(user.getDisplayName());
            existing.setProfileImage(user.getProfileImage());
            memberMapper.saveMember(existing); // 필요시 update 쿼리로 교체
            return existing;
        } else {
            user.setTiktokUserId(accessToken); // WebSocket token용
            memberMapper.saveMember(user);
            return user;
        }
    }

    private String extractAccessToken(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            return root.path("data").path("access_token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse access token", e);
        }
    }
}