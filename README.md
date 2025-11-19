# Chat-Forestfull – 포인트 및 결제 시스템 설계

## 1. 시스템 개요

* 목적: 실시간 커뮤니티에서 포인트 기반 기능 제공 및 결제/권한 관리
* 결제 수단: PayPal REST API 연동
* 주요 기능:

  * 포인트 구매
  * 임시 매니저 권한 부여
  * 결제 취소/환불 시 포인트 및 권한 회수
  * 감사 로그 기록

## 2. 포인트 & 임시 매니저 권한 구조

| 항목        | 설명                            |
| --------- | ----------------------------- |
| 포인트       | PayPal 결제 후 지급, 특별 채팅 등 사용 가능 |
| 임시 매니저 권한 | 상위 1~3위 포인트 구매자에게 1주일간 부여     |
| 권한 사용 제한  | Mute 기능: 1시간, 주 10회 제한        |
| 특별 채팅 사용  | 사진+텍스트 업로드 시 필요               |

## 3. 결제/포인트 흐름

1. **결제 완료**

   * PayPal Sandbox / Production 승인
   * DB에 결제 기록 저장 (상태: SUCCESS)
   * 포인트 지급
   * 주간 상위 포인트 구매자 순위 집계 → 임시 매니저 권한 부여

2. **결제 취소 / 환불**

   * PayPal Webhook 이벤트 수신 (`PAYMENT.CAPTURE.REFUNDED` 등)
   * DB 업데이트:

     * 포인트 차감
     * 임시 매니저 권한 회수

       * 이미 사용한 권한은 기록으로 남김
       * 추가 사용 제한
   * 사용자 알림 발송 (채팅 내 공지 또는 모달)

3. **권한 만료**

   * 주 단위 집계 후 자동 소멸
   * 사용 기록 감사 로그에 저장

## 4. 트랜잭션 설계

* 포인트 차감 + 권한 회수 **원자적 트랜잭션**
* 결제 취소 이벤트 처리 예시:

```java
@Transactional
public void handleRefund(String transactionId) {
    Payment payment = paymentRepository.findByTransactionId(transactionId);
    if(payment == null || !payment.getStatus().equals("SUCCESS")) return;

    User user = payment.getUser();
    user.decreasePoints(payment.getAmount());

    if(user.hasTemporaryManagerRole()) {
        user.revokeTemporaryManagerRole();
    }

    auditLogRepository.save(new AuditLog(user, "Refund processed", LocalDateTime.now()));
    userRepository.save(user);
}
```

## 5. PayPal Webhook 처리

* 이벤트 수신: 결제 완료, 취소, 환불 등
* 처리 흐름:

  1. Webhook 이벤트 수신
  2. 이벤트 타입 확인 (`PAYMENT.CAPTURE.COMPLETED`, `PAYMENT.CAPTURE.REFUNDED`)
  3. DB 업데이트 및 트랜잭션 실행
  4. 사용자 알림 및 로그 기록

## 6. 추가 고려 사항

* 주간 순위 집계와 결제 취소 동기화
* 포인트/권한 남용 방지 (사용 횟수, 시간 제한, 감사 로그 기록)
* 백업 및 복구 (포인트/권한 상태 정기 백업, 결제 취소/환불 이벤트 재처리 가능)

