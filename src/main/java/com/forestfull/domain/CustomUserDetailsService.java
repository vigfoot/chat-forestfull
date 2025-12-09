package com.forestfull.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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

    public List<String> getRoles(Long userId) {
        final User user = userMapper.getRolesByUserId(userId);
        if (user == null) throw new UsernameNotFoundException("User not found: " + userId);

        return user.getRoleList();
    }

    private Collection<? extends GrantedAuthority> getAuthorities(String roles) {
        return Arrays.stream(roles.split(","))
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    public boolean signup(User request) {
        request.setPassword(passwordEncoder.encode(request.getPassword()));
        return userMapper.save(request);
    }
}