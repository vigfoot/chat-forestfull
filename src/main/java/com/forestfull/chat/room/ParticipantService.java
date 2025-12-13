package com.forestfull.chat.room;

import com.forestfull.chat.ChatDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final SimpMessageSendingOperations messagingTemplate;

    // 실시간 인원 카운트 저장소 (key: roomId, value: count)
    private final ConcurrentMap<Long, Integer> participantCounts = new ConcurrentHashMap<>();

    private static final String PARTICIPANTS_TOPIC = "/topic/rooms/participants";

    /**
     * 🟢 추가된 함수: 특정 방의 현재 인원수를 조회합니다.
     */
    public int getParticipantCount(Long roomId) {
        // 맵에서 인원수를 가져오고, 값이 없으면 0을 반환합니다.
        return participantCounts.getOrDefault(roomId, 0);
    }

    /**
     * 방 생성 시 카운터 초기화
     */
    public void initializeRoomCount(Long roomId) {
        participantCounts.put(roomId, 0);
    }

    /**
     * 방 삭제 시 카운터 제거
     */
    public void removeRoomCount(Long roomId) {
        participantCounts.remove(roomId);
    }

    /**
     * 인원수 증가 및 브로드캐스팅
     */
    public void incrementParticipantCount(Long roomId) {
        // 메모리 카운터 증가 및 현재 인원수 가져오기
        Integer newCount = participantCounts.compute(roomId, (k, v) -> v == null ? 1 : v + 1);
        broadcastParticipantCount(roomId, newCount);
    }

    /**
     * 인원수 감소 및 브로드캐스팅
     */
    public void decrementParticipantCount(Long roomId) {
        // 메모리 카운터 감소 및 현재 인원수 가져오기 (0 미만 방지)
        Integer newCount = participantCounts.compute(roomId, (k, v) -> (v == null || v <= 0) ? 0 : v - 1);
        broadcastParticipantCount(roomId, newCount);
    }

    /**
     * 웹소켓으로 인원수 업데이트 메시지 전송
     */
    private void broadcastParticipantCount(Long roomId, int count) {
        // ChatDTO.RoomParticipantUpdate DTO가 필요함
        ChatDTO.RoomParticipantUpdate update = ChatDTO.RoomParticipantUpdate.builder()
                .roomId(roomId)
                .count(count)
                .build();

        messagingTemplate.convertAndSend(PARTICIPANTS_TOPIC, update);
        log.debug("Broadcasted participant update: Room {} count {}", roomId, count);
    }
}