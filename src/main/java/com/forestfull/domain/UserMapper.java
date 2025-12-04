package com.forestfull.domain;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper {

    User findByUsername(String username);

    void save(User user);

    List<User> findAllUsers();

    void updateRoles(String username, String roles);

    void deleteByUsername(String username);
}