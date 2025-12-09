package com.forestfull.domain;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    User findByUsername(String username);

    User findByUserId(Long userId);

    Long findUserIdById(Long id);

    Boolean save(User member);

    List<User> findAllUsers();

    Boolean updateRoles(@Param("id") Long id, @Param("roles") String roles);

    Boolean deleteById(Long userId);

    User getRolesByUserId(Long userId);
}