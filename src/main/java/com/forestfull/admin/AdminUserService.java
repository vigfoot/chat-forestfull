package com.forestfull.admin;

import com.forestfull.domain.User;
import com.forestfull.domain.UserMapper;
import com.forestfull.member.MemberDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserMapper userMapper;

    public List<MemberDTO.Member> getAllUsers() {
        return userMapper.findAllUsers();
    }

    public boolean updateUserRoles(Long id, String roles) {
        return userMapper.updateRoles(id, roles);
    }

    public boolean deleteUser(Long id) {
        return userMapper.deleteById(id);
    }
}