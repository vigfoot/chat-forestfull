package com.forestfull.chat;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatMessageMapper {
    void insertMessage(ChatMessageDTO.Message message);
    List<ChatMessageDTO.Message> findRecentMessages(Long roomId, int limit);
}