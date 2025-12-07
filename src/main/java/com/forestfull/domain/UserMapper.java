package com.forestfull.domain;

import com.forestfull.member.MemberDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper {

    User findByUsername(String username);

    Long findIdByUsername(String username);

    Boolean save(User user);

    List<MemberDTO.Member> findAllUsers();

    Boolean updateRoles(@Param("id") Long id, @Param("roles") String roles);

    Boolean deleteById(Long userId);
}