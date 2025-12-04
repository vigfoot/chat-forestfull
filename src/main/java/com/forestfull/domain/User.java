package com.forestfull.domain;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String roles;

    @Getter
    public static class SignUpRequest extends User{
        private final String roles = "ROLE_USER";
    }

}