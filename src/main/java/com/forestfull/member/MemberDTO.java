package com.forestfull.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class MemberDTO {
    private Long id;

    private String roles = "ROLE_USER";

    @NotBlank(message = "ID is required.")
    @Size(min = 4, max = 50, message = "ID must be between 4 and 20 characters.")
    @Pattern(regexp = "^[a-zA-Z0-9]*$", message = "ID must contain only English letters and numbers.")
    private String username;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters.")
    private String password;
    @NotBlank(message = "new Password is required.")
    @Size(min = 8, max = 50, message = "new Password must be between 8 and 50 characters.")
    private String newPassword;

    // Email Address (Optional)
    @Size(min = 10, max = 100, message = "Email Address must be between 10 and 100 characters.")
    @Pattern(regexp = "^\\S+@\\S+\\.\\S+$", message = "Not allow characters.")
    private String email;

    // Nickname
    @NotBlank(message = "Nickname is required.")
    @Size(min = 2, max = 30, message = "Nickname must be between 2 and 30 characters.")
    private String displayName;

    // Profile Image File (Optional)
    private MultipartFile profileImage;
}