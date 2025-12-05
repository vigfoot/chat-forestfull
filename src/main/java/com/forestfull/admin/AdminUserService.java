package com.forestfull.admin;

import com.forestfull.domain.User;
import com.forestfull.domain.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserMapper userMapper;

    public Flux<User> getAllUsers() {
        return Flux.just(userMapper.findAllUsers())
                .flatMapIterable(users -> users);
    }

    public Mono<Boolean> updateUserRoles(String username, String roles) {
        return Mono.just(userMapper.updateRoles(username, roles));
    }

    public Mono<Boolean> deleteUser(String username) {
        return Mono.just(userMapper.deleteByUsername(username));
    }
}