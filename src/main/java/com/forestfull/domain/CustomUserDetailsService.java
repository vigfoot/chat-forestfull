package com.forestfull.domain;

import com.forestfull.member.MemberDTO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        MemberDTO.Member member = userMapper.findByUsername(username);
        if (member == null)
            throw new UsernameNotFoundException("User not found: " + username);

        // DB에서 role 컬럼은 문자열로 "ROLE_USER"처럼 저장
        List<GrantedAuthority> authorities = Arrays.stream(member.getRole().split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return org.springframework.security.core.userdetails.User.builder()
                .username(member.getName())
                .password(member.getPassword()) // BCrypt 인코딩 되어 있다고 가정
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    public boolean signup(User.SignUpRequest request) {
        // 중복 체크
        if (userMapper.findByUsername(request.getUsername()) != null)
            return false;

        // 비밀번호 인코딩
        request.setPassword(passwordEncoder.encode(request.getPassword()));
        userMapper.save(request);
        return true;
    }
}