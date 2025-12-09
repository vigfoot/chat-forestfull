package com.forestfull.chat.message;

import com.forestfull.chat.ChatDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageMapper chatMessageMapper;

    @Transactional
    public ChatDTO.Message saveMessage(ChatDTO.Message message) {
        int res = chatMessageMapper.saveMessage(message);
        log.info("💾 Message saved: {} -> result={}", message.getMessage(), res);
        return message;
    }

    public List<ChatDTO.Message> getRecentMessages(Long roomId, int limit) {
        return chatMessageMapper.findRecentMessages(roomId, limit);
    }
}