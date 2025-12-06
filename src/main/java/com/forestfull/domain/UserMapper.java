package com.forestfull.domain;

import com.forestfull.member.MemberDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper {

    MemberDTO.Member findByUsername(String username);

    @Select("SELECT id FROM chat_forestfull.member WHERE member.name = #{username} LIMIT 1")
    Long findIdByUsername(String username);

    Boolean save(User user);

    List<MemberDTO.Member> findAllUsers();

    Boolean updateRoles(String username, String roles);

    Boolean deleteByUsername(String username);
}