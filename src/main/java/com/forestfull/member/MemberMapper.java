package com.forestfull.member;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MemberMapper {

    @Select("SELECT COUNT(0) > 0 FROM chat_forestfull.member WHERE name = #{username}")
    Boolean isExistedUsername(String username);

    @Select("SELECT COUNT(0) > 0 FROM chat_forestfull.member WHERE display_name = #{displayName}")
    Boolean isExistedNickname(String displayName);

    @Select("SELECT COUNT(0) > 0 FROM chat_forestfull.member WHERE email = #{email}")
    boolean isEmailRegistered(String email);

    int updateProfileImage(@Param("id") Long id, @Param("profileImageUrl") String profileImageUrl);

    Boolean isNicknameTakenByOtherUser(@Param("userId") Long userId, @Param("nickname") String nickname);

    Boolean isEmailTakenByOtherUser(@Param("userId") Long userId, @Param("email") String email);

    int updateProfile(@Param("userId") Long userId, @Param("newNickname") String newNickname, @Param("newEmail") String newEmail, @Param("newProfileImageUrl") String newProfileImageUrl);

    int updatePassword(@Param("userId") Long userId, @Param("encodedPassword") String encodedPassword);
}