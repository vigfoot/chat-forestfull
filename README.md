# 🌳 chat-forestfull

[![MVP Status](https://img.shields.io/badge/status-MVP-brightgreen)](https://github.com/)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

chat-forestfull은 **WebSocket 기반 실시간 커뮤니티 채팅 시스템**입니다.  
단일 Spring Boot 애플리케이션으로 구현되며, **MVP 개발을 목표**로 합니다.

---

## 🎯 프로젝트 목표

- **14일 MVP 완성**
- OAuth(TikTok) 기반 회원 관리
- 실시간 WebSocket 채팅 구현
- 사용자 인증(JWT) 적용
- 관리자 화면(Thymeleaf) 포함
- Docker 기반 실행 환경 구성 (단일 컨테이너)

> ⚠️ 확장 기능(PayPal 결제, 포인트 등)은 MVP 범위에서 제외되며, 추후 적용 예정입니다.

---

## 📌 주요 기능 (MVP 범위)

### 1️⃣ 회원가입 / 로그인
- JWT 기반 인증
- 사용자 정보 저장: 닉네임, 계정 식별 ID
- 인증된 사용자만 WebSocket 연결 가능

### 2️⃣ 실시간 채팅
- 단일 채팅방 운영
- 메시지 타입 구분:
    - 사용자 메시지
    - 시스템 메시지 (입장/퇴장)
- Mentions: `@닉네임` 하이라이트 처리

### 3️⃣ 관리자 기능
- 관리자 로그인 화면 제공
- 기능:
    - 사용자 목록 확인
    - 강퇴(Ban)
    - 채팅 제한(mute)
    - 시스템 공지 발행

### 4️⃣ 로그 기록
- 채팅 메시지: **DB(JSON) 저장**
- 시스템 이벤트: **파일 로그** 저장
- 장애 발생 시:
    - 관리자 UI 알림
    - 사용자 UI에는 최소 안내 메시지

---

## 📌 향후 확장 기능

| 기능 | 상태 |
|------|------|
| PayPal 결제 | 🚧 향후 적용 예정 |
| 포인트 시스템 | 🚧 결제 연동 완료 후 개발 |
| 임시 매니저 권한 시스템 | 🚧 확장 기능 |
| Webhook 기반 통신 | 🚧 추후 적용 |

---

## 🔒 인증 정책

- JWT 기반 인증
- WebSocket 핸드셰이크 단계에서 토큰 검증
- Refresh Token 구조 고려만 유지 (MVP는 Access Token만 사용)

---

## 🧱 기술 스택

| 영역 | 기술 |
|------|------|
| Backend | Spring Boot 3.x |
| View | Thymeleaf + Vanilla JS |
| DB | MariaDB (MyBatis 기반) |
| Realtime | WebSocket (STOMP 없이 커스텀 핸들러) |
| Auth | JWT |
| Deployment | Docker 단일 컨테이너 |

---

## 🧩 단일 인스턴스 구조

```text
[ Client Browser ]
         ↕ (HTTPS / WebSocket)
[ Nginx Reverse Proxy ]
         ↕
[ Spring Boot Application ]
         ↕
[ MariaDB ]
```
