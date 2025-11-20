# \# Chat-Forestfull – Architecture \& Deployment

# 

# \## 1. 시스템 아키텍처

# 

# \### 1.1 서버 구조

# \- 단일 인스턴스: Frontend + Backend 통합

# \- Spring Boot WebFlux 기반

# \- JWT 인증 사용 (액세스 토큰 30분, 리프레시 토큰 14일)

# \- WebSocket 기반 실시간 채팅

# \- 관리자 UI: Thymeleaf 템플릿 기반

# 

# \### 1.2 데이터베이스

# \- MariaDB 사용 (R2DBC 대신 JDBC로 구현)

# \- DB 스키마 예시:

# &nbsp; - users: 사용자 정보, TikTok OAuth 데이터

# &nbsp; - messages: 채팅 메시지, 멘션, 타임스탬프

# &nbsp; - payments: 결제 내역, 포인트, 상태

# &nbsp; - audit\_logs: 권한 사용, 관리자 조작 기록

# \- Redis 캐싱 없음

# 

# \### 1.3 결제/포인트

# \- PayPal REST API 연동

# \- Webhook 이벤트 수신 (결제 완료, 취소, 환불)

# \- Signature 검증 포함

# \- 상태 관리(State Machine): SUCCESS, FAILED, CANCELLED, REFUNDED

# 

# \### 1.4 WebSocket / SSE

# \- WebSocket: 실시간 채팅

# &nbsp; - JWT 핸드셰이크 인증

# &nbsp; - 최대 동시 접속: 100명

# &nbsp; - 연결 유지: Heartbeat ping/pong, 자동 재연결

# \- SSE: 알림/공지용 병행 가능

# 

# \## 2. 배포 환경

# 

# \### 2.1 서버

# \- 개인 노트북 GE40에서 호스팅

# \- Docker 컨테이너 사용

# &nbsp; - Spring Boot: 443 → 8080 매핑

# \- nginx: 이미 설치되어 있으며 HTTPS 인증서 존재

# 

# \### 2.2 인증서 및 HTTPS

# \- Let's Encrypt 인증서 자동 갱신 불필요 (nginx에서 이미 관리)

# \- HTTPS 지원, 별도 구성 없음

# 

# \### 2.3 로드 밸런싱

# \- 단일 노트북 환경이라 제한적

# \- JWT 기반이므로 다중 컨테이너 확장 가능

# \- 추후 Docker Swarm / Kubernetes 고려 가능

# 

# \### 2.4 로그 및 백업

# \- 로그: 결제 및 채팅 이벤트 파일 저장 + DB 저장

# \- 장애 알림: 서버 모달 + 이메일

# \- 백업/복구: 선택 사항, 추후 구현

# 

# \## 3. 주요 고려 사항

# \- 동시성 문제 처리

# \- 결제 취소/환불 처리 안정화

# \- WebSocket 연결 유지 정책

# \- 관리자 UI 완성도 유지

# \- 성능 테스트 수행

# 

