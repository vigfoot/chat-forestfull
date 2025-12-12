package com.forestfull.member;

import com.forestfull.common.file.FileService;
import com.forestfull.domain.User;
import com.forestfull.domain.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final FileService fileService;
    private final UserMapper userMapper;
    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    // (기존 회원가입 및 중복 확인 메서드는 변경 없음)
    public boolean isExistedUsername(String username) {
        return Boolean.TRUE.equals(memberMapper.isExistedUsername(username));
    }

    public boolean isExistedNickname(String displayName) {
        return Boolean.TRUE.equals(memberMapper.isExistedNickname(displayName));
    }

    public boolean isEmailRegistered(String email) {
        return memberMapper.isEmailRegistered(email);
    }

    public void updateProfileImage(Long userId, String profileImageUrl) {
        if (userId == null || userId <= 0 || !StringUtils.hasText(profileImageUrl)) return;

        memberMapper.updateProfileImage(userId, profileImageUrl);
    }

    // ---------------------------------------------------------------------------------
    // [ 마이페이지 - 유효성 검증 로직 최적화 ]
    // ---------------------------------------------------------------------------------
    /**
     * 닉네임이 현재 사용자의 것인지, 혹은 다른 사용자에게 등록되지 않았는지 확인합니다.
     * 🚩 최적화: 닉네임 변경 여부 확인만 DB 조회 (UserMapper.findByUserId 호출 필요)
     */
    public boolean isNicknameAvailableForUpdate(Long currentUserId, String newNickname) {
        // 1. 현재 사용자 조회
        final User currentUser = userMapper.findByUserId(currentUserId);
        if (currentUser == null) throw new IllegalArgumentException("User not found.");

        // 2. 닉네임이 변경되지 않았다면 사용 가능 (DB 호출 불필요)
        if (Objects.equals(currentUser.getDisplayName(), newNickname))
            return true;

        // 3. 닉네임이 변경되었다면, 다른 사용자에게 사용 중인지 DB를 통해 확인 (MemberMapper 호출)
        return !memberMapper.isNicknameTakenByOtherUser(currentUserId, newNickname);
    }

    /**
     * 업데이트하려는 이메일이 현재 사용자의 이메일과 다른지 확인합니다.
     */
    public boolean isNewEmail(Long currentUserId, String newEmail) {
        final User currentUser = userMapper.findByUserId(currentUserId);
        if (currentUser == null)
            throw new IllegalArgumentException("User not found.");

        return !Objects.equals(currentUser.getEmail(), newEmail);
    }

    /**
     * 새 이메일이 현재 사용자를 제외한 다른 사용자에게 등록되어 있는지 확인합니다.
     */
    public boolean isEmailRegisteredByOtherUser(Long currentUserId, String email) {
        return memberMapper.isEmailTakenByOtherUser(currentUserId, email);
    }

    // ---------------------------------------------------------------------------------
    // [ 마이페이지 - 1. Profile Update ]
    // ---------------------------------------------------------------------------------
    @Transactional
    public void updateProfile(Long userId, String newNickname, String newEmail, MultipartFile profileImage) {
        final User user = userMapper.findByUserId(userId);
        if (user == null) throw new IllegalArgumentException("User not found.");

        String newProfileImageUrl = user.getProfileImage();

        // 2. 파일 처리 및 URL 업데이트 (프로필 이미지가 제공된 경우)
        if (profileImage != null && !profileImage.isEmpty()) {
            // 🚩 규칙 1 반영: 기존 파일을 삭제하는 로직은 제거 (보관)

            final File file = fileService.saveProfileImage(profileImage, userId);

            if (!file.exists()) throw new RuntimeException("Failed to save new profile image.");

            newProfileImageUrl = "/file/profiles/" + userId + "/" + file.getName();
        }

        memberMapper.updateProfile(userId, newNickname, newEmail, newProfileImageUrl);
    }

    // ---------------------------------------------------------------------------------
    // [ 마이페이지 - 2. Change Password ]
    // ---------------------------------------------------------------------------------

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        final User user = userMapper.findByUserId(userId);

        if (user == null)
            throw new IllegalArgumentException("User not found.");

        // 1. 현재 비밀번호 일치 확인
        if (!passwordEncoder.matches(currentPassword, user.getPassword()))
            throw new SecurityException("Current password is incorrect.");

        // 2. 새 비밀번호 인코딩 및 업데이트
        String encodedNewPassword = passwordEncoder.encode(newPassword);
        memberMapper.updatePassword(userId, encodedNewPassword);
    }

    // ---------------------------------------------------------------------------------
    // [ 마이페이지 - 3. Delete Account ]
    // ---------------------------------------------------------------------------------
    @Transactional
    public void deleteUser(Long userId) {
        final User user = userMapper.findByUserId(userId);
        if (user == null) return;

        userMapper.deleteById(userId);
    }
}