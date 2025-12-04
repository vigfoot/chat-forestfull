package com.forestfull.admin;

import com.forestfull.domain.User;
import com.forestfull.domain.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserMapper userMapper;

    public List<User> getAllUsers() {
        return userMapper.findAllUsers();
    }

    public void updateUserRoles(String username, String roles) {
        userMapper.updateRoles(username, roles);
    }

    public void deleteUser(String username) {
        userMapper.deleteByUsername(username);
    }
}