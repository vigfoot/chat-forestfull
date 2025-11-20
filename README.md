# Chat-Forestfull

## 프로젝트 개요
Chat-Forestfull은 실시간 커뮤니티를 제공하는 웹 애플리케이션입니다.  
틱톡 팔로워를 기반으로 한 커뮤니티 기능과 포인트 시스템을 결합하여, 사용자에게 특별 채팅 기능과 임시 매니저 권한을 제공합니다.

---

## 주요 기능

### 1. 회원가입 / 로그인
- TikTok OAuth 전용 로그인
- JWT 기반 인증 (액세스 토큰 30분 / 리프레시 토큰 14일)
- 사용자 정보: 닉네임, TikTok ID, 팔로우 정보

### 2. 실시간 채팅
- 단일 채팅방, 선별된 사용자만 접근
- WebSocket 기반 실시간 메시지 전송
- 멘션 기능: `@username` 하이라이트
- 특별 채팅: 사진 + 텍스트 업로드 (임시 매니저 권한 필요)

### 3. 포인트 & 임시 매니저
- PayPal 결제 연동
- 포인트 구매 순위 상위 1~3위 사용자에게 1주일간 임시 매니저 권한
- 권한: mute 기능(1시간, 주 10회), 특별 채팅 가능

### 4. 관리자 기능
- 사용자 관리: 가입 승인, 강퇴, mute, 임시 매니저 권한 관리
- 채팅 모니터링: 메시지 스트림 확인, 금칙어 필터링
- 공지 발행: 상단 배너, 모달 팝업
- 결제/포인트 관리: 결제 로그 확인, 순위 집계
- 로그 기록: 파일 + DB, 장애/결제 오류 알림

### 5. 기타
- 단일 인스턴스 운영 (Frontend + Backend 통합)
- Docker 컨테이너화
- Nginx와 기존 HTTPS 인증 활용

---

## 개발 환경
- **Backend:** Spring Boot WebFlux, JWT, WebSocket
- **DB:** MariaDB
- **Frontend:** Thymeleaf
- **결제:** PayPal REST API + Webhook
- **배포:** Docker on GE40 노트북

---

## 21일 개발 일정
자세한 일정은 [`CHAT_FORESTFULL_21DAY_PLAN.md`](./PLAN.md) 참조.
