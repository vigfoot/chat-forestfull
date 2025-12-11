package com.forestfull.member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    public boolean isExistedUsername(String username) {
        return Boolean.TRUE.equals(memberMapper.isExistedUsername(username));
    }

    public boolean isExistedNickname(String displayName) {
        return Boolean.TRUE.equals(memberMapper.isExistedNickname(displayName));
    }

    public void updateProfileImage(Long userId, String profileImageUrl) {
        if (userId == null || userId <= 0 || !StringUtils.hasText(profileImageUrl)) return;

        memberMapper.updateProfileImage(userId, profileImageUrl);
    }
}