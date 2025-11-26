package com.forestfull.chat;

import com.forestfull.member.MemberDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChatRoomMapper {
    void joinRoom(MemberDTO.Room memberRoom);

    void leaveRoom(@Param("roomId") Long roomId, @Param("memberId") Long memberId);
}