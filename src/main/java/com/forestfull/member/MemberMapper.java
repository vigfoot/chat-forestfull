package com.forestfull.member;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MemberMapper {

    void saveMember(MemberDTO.Member member);

    MemberDTO.Member findByTikTokUserId(@Param("tiktokUserId") String tiktokUserId);
}