package com.forestfull.domain;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM chat_forestfull.member WHERE tiktok_user_id = #{username}")
    User findByUsername(String username);

    @Insert("INSERT INTO chat_forestfull.member (tiktok_user_id, password, role) VALUES (#{username}, #{password}, #{roles})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void save(User user);
}