package com.forestfull.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String name) throws UsernameNotFoundException {
        final User user = userMapper.findByUsername(name);
        if (user == null) throw new UsernameNotFoundException("User not found: " + name);

        return user;
    }

    public User loadUserByUserId(Long userId) throws UsernameNotFoundException {
        final User user = userMapper.findByUserId(userId);
        if (user == null) throw new UsernameNotFoundException("User not found: " + userId);

        return user;
    }

    public User signup(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.save(user);
        return user;
    }

    public boolean isExistedUsername(String username) {
        return Boolean.TRUE.equals(userMapper.isExistedUsername(username));
    }

    public boolean isExistedNickname(String displayName) {
        return Boolean.TRUE.equals(userMapper.isExistedNickname(displayName));
    }

    public void updateProfileImage(Long userId, String profileImageUrl) {
        if (userId == null || userId <= 0 || !StringUtils.hasText(profileImageUrl)) return;

        userMapper.updateProfileImage(userId, profileImageUrl);
    }
}