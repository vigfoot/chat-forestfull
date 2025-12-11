package com.forestfull.member;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MemberMapper {

    @Select("SELECT COUNT(0) > 0 FROM chat_forestfull.member WHERE name = #{username}")
    Boolean isExistedUsername(String username);

    @Select("SELECT COUNT(0) > 0 FROM chat_forestfull.member WHERE display_name = #{displayName}")
    Boolean isExistedNickname(String displayName);

    @Insert("UPDATE chat_forestfull.member SET profile_image = #{profileImageUrl} WHERE id = #{id}")
    int updateProfileImage(Long id, String profileImageUrl);
}
