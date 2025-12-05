package com.forestfull.domain;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {

    User findByUsername(String username);

    Boolean save(User user);

    List<User> findAllUsers();

    Boolean updateRoles(String username, String roles);

    Boolean deleteByUsername(String username);
}