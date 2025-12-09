package com.forestfull.chat.room;

import com.forestfull.chat.ChatDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatRoomMapper {

    List<ChatDTO.Room> findAllRooms();

    ChatDTO.Room findRoomById(@Param("roomId") Long roomId);

    int createRoom(ChatDTO.Room room);

    int deleteRoom(@Param("roomId") Long roomId);

    List<ChatDTO.Participant> findParticipants(@Param("roomId") Long roomId);

    boolean isUserInRoom(@Param("roomId") Long roomId, @Param("memberId") Long memberId);

    int addParticipant(@Param("roomId") Long roomId,
                       @Param("memberId") Long memberId);

    int removeParticipant(@Param("roomId") Long roomId,
                          @Param("memberId") Long memberId);
}