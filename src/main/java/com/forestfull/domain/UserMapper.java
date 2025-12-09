package com.forestfull.domain;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    User findByUsername(String username);

    Long findIdByUsername(String username);

    Boolean save(MemberDTO.Member member);

    List<MemberDTO.Member> findAllUsers();

    Boolean updateRoles(@Param("id") Long id, @Param("roles") String roles);

    Boolean deleteById(Long userId);
}