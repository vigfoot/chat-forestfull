package com.forestfull.domain;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM chat_forestfull.member WHERE tiktok_user_id = #{username}")
    User findByUsername(String username);
}