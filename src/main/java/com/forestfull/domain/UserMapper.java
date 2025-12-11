package com.forestfull.domain;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper {

    User findByUsername(String username);

    User findByUserId(Long userId);

    Long findUserIdById(Long id);

    List<User> findAllUsers();

    Boolean save(User user);

    Boolean updateRoles(@Param("id") Long id, @Param("roles") String roles);

    Boolean deleteById(Long userId);
}