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

    Boolean save(User member);

    List<User> findAllUsers();

    Boolean updateRoles(@Param("id") Long id, @Param("roles") String roles);

    Boolean deleteById(Long userId);

    User getRolesByUserId(Long userId);

    @Select("SELECT COUNT(0) > 0 FROM chat_forestfull.member WHERE name = #{username}")
    Boolean isExistedUsername(String username);

    @Select("SELECT COUNT(0) > 0 FROM chat_forestfull.member WHERE display_name = #{displayName}")
    Boolean isExistedNickname(String displayName);

    @Insert("UPDATE chat_forestfull.member SET profile_image = #{profileImageUrl} WHERE id = #{id}")
    int updateProfileImage(Long id, String profileImageUrl);
}