package com.forestfull.common.token;

import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;

@Mapper
public interface RefreshTokenMapper {

    @Insert("""
        INSERT INTO chat_forestfull.refresh_token(member_id, token, expiry_date, is_revoked)
        VALUES(#{memberId}, #{token}, #{expiryDate}, 0)
    """)
    int save(@Param("memberId") Long memberId,
               @Param("token") String token,
               @Param("expiryDate") LocalDateTime expiryDate);

    @Select("""
        SELECT token
        FROM chat_forestfull.refresh_token
        WHERE member_id = #{memberId}
          AND is_revoked = 0
          AND expiry_date > NOW()
        ORDER BY expiry_date DESC
        LIMIT 1
    """)
    String findValidTokenByMemberId(@Param("memberId") Long memberId);

    @Select("""
        SELECT member_id
        FROM chat_forestfull.refresh_token
        WHERE token = #{token}
          AND is_revoked = 0
        LIMIT 1
    """)
    Long findMemberIdByToken(@Param("token") String token);

    @Update("""
        UPDATE chat_forestfull.refresh_token
        SET is_revoked = 1
        WHERE member_id = #{id}
    """)
    int revokeByMemberId(@Param("id") Long id);

    @Delete("""
        DELETE FROM chat_forestfull.refresh_token
        WHERE member_id = #{memberId}
    """)
    int deleteByMemberId(@Param("memberId") Long memberId);
}
