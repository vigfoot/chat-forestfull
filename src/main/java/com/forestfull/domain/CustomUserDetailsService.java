package com.forestfull.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public User signup(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.save(user);
        return user;
    }

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
}