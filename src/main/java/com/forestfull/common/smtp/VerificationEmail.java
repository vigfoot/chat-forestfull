package com.forestfull.common.smtp;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationEmail {
    @Size(min = 10, max = 100, message = "Email Address must be between 10 and 100 characters.")
    @Pattern(regexp = "^\\S+@\\S+\\.\\S+$", message = "Not allow characters.")
    private String email;

    @Size(max = 6, min = 6)
    @Pattern(regexp = "^\\d{6}$", message = "Code must be 6 digits.")
    private String code;

    // 이 필드는 DTO로 사용될 때 요청에 포함되지 않으므로 @JsonIgnore 등으로 보호하는 것이 좋습니다.
    private LocalDateTime expiryTime;
}