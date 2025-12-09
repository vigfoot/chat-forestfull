package com.forestfull.chat;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    int saveMessage(ChatDTO.Message message);

    List<ChatDTO.Message> findRecentMessages(
            @Param("roomId") Long roomId,
            @Param("limit") int limit
    );
}
