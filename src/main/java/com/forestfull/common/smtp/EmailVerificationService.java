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
    private final Random random = new Random();

    // 🚩 Configuration Values from application.yml
    @Value("${app.email.verification-code-length:6}")
    private int codeLength;

    @Value("${app.email.verification-timeout-seconds:180}")
    private long codeTimeoutSeconds; // Verification code validity (e.g., 3 minutes)

    @Value("${app.email.verification-success-timeout-seconds:600}")
    private long successTimeoutSeconds; // Verified status validity (e.g., 10 minutes)

    @Value("${app.email.from-address}")
    private String fromAddress;

    // 🚩 Store 1: Stores the code and its expiry time for verification
    // Key: email address, Value: VerificationEmail (contains code and expiryTime)
    private final ConcurrentMap<String, VerificationEmail> verificationStore = new ConcurrentHashMap<>();

    // 🚩 Store 2: Stores the final 'verified' status for signup eligibility
    // Key: email address, Value: Verified status expiry time
    private final ConcurrentMap<String, LocalDateTime> verifiedEmailStore = new ConcurrentHashMap<>();


    /**
     * Generates a random numeric verification code.
     * @return The generated code string
     */
    private String generateRandomCode() {
        int min = (int) Math.pow(10, codeLength - 1);
        int max = (int) Math.pow(10, codeLength) - 1;
        int codeInt = random.nextInt(max - min + 1) + min;
        return String.format("%0" + codeLength + "d", codeInt);
    }

    /**
     * Generates a code, saves it to the verification store, and sends it via email.
     *
     * @param email The recipient's email address
     * @throws MessagingException Thrown if mail sending fails
     */
    public void sendVerificationCode(String email) throws MessagingException {
        // 1. Code generation and expiry time setting
        String code = generateRandomCode();
        LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(codeTimeoutSeconds);

        // 2. Save to verification store (overwrites existing code for resend logic)
        verificationStore.put(email, VerificationEmail.builder()
                .email(email)
                .code(code)
                .expiryTime(expiryTime).build());
        log.info("Verification code generated for {}: {}", email, code);

        // 3. Email sending
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromAddress);
        helper.setTo(email);
        helper.setSubject("[Chat ForestFull] Email Verification Code");

        // HTML email content
        String htmlContent = buildEmailContent(code, codeTimeoutSeconds / 60);

        helper.setText(htmlContent, true);

        mailSender.send(message);
        log.info("Verification email sent to {}", email);
    }

    /**
     * Verifies the user-input code against the stored code.
     * On success, marks the email as verified for signup eligibility.
     *
     * @param email The email address to verify
     * @param code The code input by the user
     * @return True if verification is successful, false otherwise
     */
    public boolean verifyCode(String email, String code) {
        VerificationEmail data = verificationStore.get(email);

        // 1. Check if data exists or if the code has expired
        if (data == null || LocalDateTime.now().isAfter(data.getExpiryTime())) {
            verificationStore.remove(email); // Clean up expired data
            log.warn("Verification attempt failed for {}: Code not found or expired.", email);
            return false;
        }

        // 2. Check for code match
        if (data.getCode().equals(code)) {
            verificationStore.remove(email); // Remove code from store 1 (one-time use)

            // 3. Mark as verified in Store 2
            LocalDateTime successExpiryTime = LocalDateTime.now().plusSeconds(successTimeoutSeconds);
            verifiedEmailStore.put(email, successExpiryTime);

            log.info("Email {} successfully verified and marked for signup.", email);
            return true;
        }

        log.warn("Verification attempt failed for {}: Code mismatch.", email);
        return false;
    }

    /**
     * Checks if the email is currently marked as verified and eligible for signup.
     * Used by the /api/auth/signup endpoint.
     */
    public boolean isVerifiedForSignup(String email) {
        LocalDateTime expiryTime = verifiedEmailStore.get(email);

        if (expiryTime == null) {
            return false; // Not verified or already removed
        }

        if (LocalDateTime.now().isAfter(expiryTime)) {
            // Expired, clean up and return false
            verifiedEmailStore.remove(email);
            log.warn("Signup attempt failed for {}: Verified status expired.", email);
            return false;
        }

        return true; // Validly verified
    }

    /**
     * Removes the verified status after successful user registration.
     * Used by the /api/auth/signup endpoint.
     */
    public void removeVerificationStatus(String email) {
        verifiedEmailStore.remove(email);
        log.info("Verified status removed for {} after successful signup.", email);
    }

    /**
     * Simple HTML email template builder.
     */
    private String buildEmailContent(String code, long minutes) {
        return "<div style='font-family: Arial, sans-serif; padding: 20px; border: 1px solid #eee; max-width: 600px; margin: auto;'>"
                + "<h2 style='color: #21b021;'>Chat ForestFull Email Verification</h2>"
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