package com.forestfull.domain;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class User {
    private Long id;
    private String name;
    private String password;
    private String roles;

    @Setter
    @Getter
    public static class SignUpRequest extends User {
        private final String roles = "ROLE_USER";
    }
}