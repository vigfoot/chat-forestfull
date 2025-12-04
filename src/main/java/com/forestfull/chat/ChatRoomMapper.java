package com.forestfull.chat;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatRoomMapper {

    List<ChatDTO.Room> findAll();

    ChatDTO.Room findById(Long id);

    Boolean existsMemberInRoom(@Param("roomId") Long roomId, @Param("memberId") Long memberId);

    void insertRoom(ChatDTO.Room room);

    void insertMemberRoom(@Param("roomId") Long roomId, @Param("memberId") Long memberId);

    void deleteMemberRoom(@Param("roomId") Long roomId, @Param("memberId") Long memberId);

    List<ChatDTO.Participant> findParticipants(Long roomId);
}
