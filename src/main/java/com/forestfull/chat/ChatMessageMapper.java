package com.forestfull.chat;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatMessageMapper {
    List<ChatDTO.Message> findByRoomId(Long roomId);

    void insertMessage(ChatDTO.Message message);

    List<ChatDTO.Message> findRecentMessages(Long roomId, int limit);
}