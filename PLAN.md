# Chat-Forestfull – 21일 개발 일정표

## Week 1 – 환경 세팅 & 인증/회원 관리

| Day | 목표 | 상세 내용 |
|-----|-----|---------|
| 1 | 개발 환경 준비 | - Docker 설치/테스트<br>- GE40 노트북에서 컨테이너 환경 점검<br>- Git 초기화, 폴더 구조 설계 (backend, frontend, db, docker) |
| 2 | Spring Boot 프로젝트 초기화 | - WebFlux, JWT 의존성 추가<br>- 기본 Controller, Service, Repository 구조 생성 |
| 3 | DB 세팅 | - MariaDB 설치 및 접속 확인<br>- DB 스키마 설계 (사용자 테이블, 메시지 테이블, 포인트 테이블) |
| 4 | TikTok OAuth 연동 | - OAuth 로그인 테스트<br>- 사용자 정보 가져오기 (닉네임, TikTok ID, 팔로우 여부)<br>- JWT 발급 (액세스 30분 / 리프레시 14일) |
| 5 | JWT 인증 필터 | - Spring Security + JWT 필터 구현<br>- 인증 테스트 |
| 6 | 회원가입/로그인 통합 | - OAuth + JWT 인증 완성<br>- 신규 가입 시 승인 여부 처리 |
| 7 | 간단 UI 연동 | - 로그인/회원가입 화면 (Thymeleaf) 기본 구현 |

---

## Week 2 – 실시간 채팅 & 관리자 기능

| Day | 목표 | 상세 내용 |
|-----|-----|---------|
| 8 | WebSocket 기초 | - `/ws/chat` 엔드포인트 구현<br>- 텍스트 메시지 전송/브로드캐스트 테스트 |
| 9 | 채팅 DB 연동 | - 메시지 DB 저장 구현<br>- 이전 메시지 조회 기능 구현 |
| 10 | 멘션 & 특별 채팅 | - `@username` 멘션 하이라이트<br>- 임시 매니저 전용 사진+텍스트 업로드 |
| 11 | SSE / 공지 기능 | - `/sse/notification` 구현<br>- 상단 배너 / 모달 팝업 공지 표시 |
| 12 | 관리자 UI 기본 | - `/admin/dashboard` / `/admin/users` / `/admin/chat` 생성<br>- 간단한 테이블 및 필터 적용 |
| 13 | 사용자 관리 | - 강퇴, mute, 임시 매니저 권한 발급/회수 구현 |
| 14 | 채팅 모니터링 | - 실시간 메시지 스트림 표시<br>- 금칙어 필터링 및 악성 메시지 감지 |

---

## Week 3 – 결제/포인트 + 안정화

| Day | 목표 | 상세 내용 |
|-----|-----|---------|
| 15 | PayPal 결제 연동 | - Sandbox 테스트<br>- 결제 완료 → 포인트 지급<br>- 결제 실패 처리 |
| 16 | PayPal Webhook | - Webhook 이벤트 수신<br>- 포인트/임시 매니저 권한 회수<br>- Signature 검증 구현 |
| 17 | 결제 취소/환불 | - 상태 관리 (SUCCESS / FAILED / CANCELED / REFUNDED)<br>- DB 업데이트, 사용자 알림 처리 |
| 18 | 동시성 & 오류 처리 | - WebSocket 메시지 순서 보장<br>- 결제 동시성 처리<br>- 재시도 정책 적용 |
| 19 | 로그 & 알림 | - 파일 + DB 로그 구현<br>- 서버 장애/결제 오류 시 이메일 알림 및 모달 알림 |
| 20 | Docker 컨테이너화 | - Spring Boot + MariaDB + Nginx 통합<br>- 로컬에서 배포 테스트 |
| 21 | 최종 테스트 & 성능 | - 30명 동시 접속 테스트<br>- 메시지 전송/공지/결제 흐름 확인<br>- 버그 수정, README.md 및 문서 정리 |

---

### Tip
- Week1: 환경 + 인증/회원
- Week2: 실시간 채팅 + 관리자
- Week3: 결제/포인트 + 안정화

**하루 단위로 개발 목표를 체크**하면서 진행하면 21일 안에 MVP 수준까지 완성 가능.
