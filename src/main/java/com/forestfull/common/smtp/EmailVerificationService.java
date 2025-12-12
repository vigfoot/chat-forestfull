package com.forestfull.common.smtp;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private final JavaMailSender mailSender;
    private final Random random = new Random(); // Random 인스턴스를 재사용합니다.

    @Value("${app.email.verification-code-length:6}")
    private int codeLength;

    @Value("${app.email.verification-timeout-seconds:180}")
    private long timeoutSeconds;

    @Value("${app.email.from-address}")
    private String fromAddress;

    private final ConcurrentMap<String, VerificationEmail> verificationStore = new ConcurrentHashMap<>();

    /**
     * 🚩 MODIFIED: 무작위 인증 코드를 생성하는 방식 최적화 (6자리 기준)
     */
    private String generateRandomCode() {
        // 6자리 코드를 생성하는 표준 방식 (더 간결함)
        int min = (int) Math.pow(10, codeLength - 1);
        int max = (int) Math.pow(10, codeLength) - 1;
        int codeInt = random.nextInt(max - min + 1) + min;

        // 길이에 맞춰 포맷팅
        return String.format("%0" + codeLength + "d", codeInt);
    }

    /**
     * 인증 코드를 생성하고 이메일로 발송합니다.
     */
    public void sendVerificationCode(String email) throws MessagingException {
        String code = generateRandomCode();
        LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(timeoutSeconds);

        // 인증 스토어에 저장
        verificationStore.put(email, VerificationEmail.builder()
                .email(email) // email 필드도 저장하는 것이 추후 디버깅에 유리
                .code(code)
                .expiryTime(expiryTime).build());
        log.info("Verification code generated for {}: {}", email, code);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromAddress);
        helper.setTo(email);
        helper.setSubject("[ForestFull Chat] Email Verification Code");

        String htmlContent = buildEmailContent(code, timeoutSeconds / 60);

        helper.setText(htmlContent, true);

        mailSender.send(message);
        log.info("Verification email sent to {}", email);
    }

    /**
     * 인증 코드를 검증합니다.
     */
    public boolean verifyCode(String email, String code) {
        VerificationEmail data = verificationStore.get(email);

        if (data == null) {
            log.warn("Verification attempt failed for {}: Code not found.", email);
            return false;
        }

        // 🚩 MODIFIED: 만료 확인 로직
        if (LocalDateTime.now().isAfter(data.getExpiryTime())) {
            verificationStore.remove(email);
            log.warn("Verification attempt failed for {}: Code expired.", email);
            return false;
        }

        if (data.getCode().equals(code)) {
            verificationStore.remove(email);
            log.info("Email {} successfully verified.", email);
            return true;
        }

        log.warn("Verification attempt failed for {}: Code mismatch.", email);
        return false;
    }

    /**
     * 간단한 HTML 메일 템플릿입니다.
     */
    private String buildEmailContent(String code, long minutes) {
        return "<div style='font-family: Arial, sans-serif; padding: 20px; border: 1px solid #eee; max-width: 600px; margin: auto;'>"
                + "<h2 style='color: #21b021;'>ForestFull Chat Email Verification</h2>"
                + "<p>Thank you for signing up! Please use the following code to verify your email address:</p>"
                + "<div style='background: #f4f4f4; padding: 15px; text-align: center; border-radius: 5px; font-size: 24px; font-weight: bold; letter-spacing: 5px;'>"
                + code
                + "</div>"
                + "<p style='color: #777;'>This code is valid for " + minutes + " minutes.</p>"
                + "<p>If you did not request this, please ignore this email.</p>"
                + "<p>Best regards,<br>The ForestFull Team</p>"
                + "</div>";
    }
}