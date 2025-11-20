# Chat-Forestfull – WebSocket & SSE Design

## 1. 목적
- 실시간 채팅 및 공지/알림 기능 제공
- 브라우저 기반 클라이언트 지원 (TikTok 내 웹뷰 포함)
- SSE 또는 WebSocket 연결 안정성 확보

## 2. WebSocket 설계

### 2.1 연결 구조
- 단일 엔드포인트 `/ws/chat`
- JWT 기반 인증 (핸드셰이크 시 토큰 확인)
- 최대 동시 접속: 100명 예상
- Heartbeat Ping/Pong: 연결 유지 및 끊김 감지
- 자동 재연결: 클라이언트에서 재시도

### 2.2 메시지 구조
- 채팅 메시지
  - id, senderId, content, timestamp
  - 멘션: `@username` 패턴 감지 후 하이라이트 처리
- 특별 채팅 (임시 매니저 전용)
  - 사진 + 텍스트 업로드
- 공지 메시지
  - 서버 브로드캐스트, 채팅 상단 고정 배너 표시

### 2.3 동시성 & 오류 처리
- 메시지 전송 시 DB 저장 후 브로드캐스트
- 실패 시 재시도 및 클라이언트 알림
- 동시 전송으로 인한 메시지 순서 보장 필요

## 3. SSE 설계

### 3.1 연결 구조
- 단일 엔드포인트 `/sse/notification`
- 인증 필요 없음 (읽기 전용 공지)
- 브라우저에서 이벤트 스트림 수신
- 클라이언트 자동 재연결 (retry 설정)

### 3.2 메시지 구조
- 공지 메시지
  - id, title, content, timestamp
  - 중요도: normal / high
- 중요 공지(high)는 모달 팝업 표시
- 일반 공지(normal)는 상단 배너 표시

### 3.3 고려 사항
- 브라우저 내 SSE 연결 유지 제한
- 네트워크 끊김 시 클라이언트 재연결
- 서버 이벤트 버퍼: 최근 N개 메시지 보관 (신규 접속자 전송용)

## 4. 기술 스택 & 라이브러리
- Spring Boot WebFlux
- WebSocket: `spring-websocket`
- SSE: `SseEmitter` 사용
- 메시지 직렬화: JSON
- 클라이언트: Vanilla JS 또는 lightweight framework
