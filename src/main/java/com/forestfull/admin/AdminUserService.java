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

    public boolean updateUserRoles(String username, String roles) {
        return userMapper.updateRoles(username, roles);
    }

    public boolean deleteUser(String username) {
        return userMapper.deleteByUsername(username);
    }
}